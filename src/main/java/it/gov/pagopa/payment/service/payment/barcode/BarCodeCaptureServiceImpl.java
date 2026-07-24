package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.PointOfSaleNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class BarCodeCaptureServiceImpl implements BarCodeCaptureService {

    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository repository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper mapper;
    private final AuditUtilities auditUtilities;
    private final TransactionSynchronizer transactionSynchronizer;
    private final MerchantConnector merchantConnector;

    public BarCodeCaptureServiceImpl(TransactionRepository transactionRepository,
                                     TransactionInProgressRepository repository,
                                     TransactionBarCodeInProgress2TransactionResponseMapper mapper,
                                     AuditUtilities auditUtilities,
                                     TransactionSynchronizer transactionSynchronizer,
                                     MerchantConnector merchantConnector) {
        this.transactionRepository = transactionRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
        this.transactionSynchronizer = transactionSynchronizer;
        this.merchantConnector = merchantConnector;
    }

    @Override
    public TransactionBarCodeResponse capturePayment(String initiativeId, String trxCode, String merchantId, String pointOfSaleId, String acquirerId) {
        try {
            log.info("[CAPTURE_PAYMENT] START - initiativeId={}, trxCode={}, merchantId={}, pointOfSaleId={}, acquirerId={}",
                    Utilities.sanitizeString(initiativeId),
                    Utilities.sanitizeString(trxCode),
                    Utilities.sanitizeString(merchantId),
                    Utilities.sanitizeString(pointOfSaleId),
                    Utilities.sanitizeString(acquirerId));

            String normalizedTrxCode = trxCode == null
                    ? null
                    : trxCode.toLowerCase(Locale.ROOT);

            if (normalizedTrxCode == null) {
                throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode));
            }

            TransactionInProgress trx = repository.findByTrxCode(normalizedTrxCode)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            Transaction transaction = transactionRepository.findByTrxCode(normalizedTrxCode)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,trx.getStatus()));
            }

            validateCaptureRequest(trx, initiativeId, merchantId, pointOfSaleId);

            log.info("[CAPTURE_PAYMENT] Merchant/POS onboarding checks - merchantId={}, pointOfSaleId={}, initiativeId={}",
                    Utilities.sanitizeString(merchantId),
                    Utilities.sanitizeString(pointOfSaleId),
                    Utilities.sanitizeString(initiativeId));
            merchantConnector.merchantDetail(merchantId, initiativeId);
            merchantConnector.getPointOfSale(merchantId, pointOfSaleId, initiativeId);
            log.info("[CAPTURE_PAYMENT] Checks Passed");

            deleteUnusedVouchers(trx);
            deleteUnusedVouchers(transaction);

            trx.setStatus(SyncTrxStatus.CAPTURED);
            trx.setElaborationDateTime(LocalDateTime.now(ZoneOffset.UTC));
            trx.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
            repository.save(trx);

            transactionSynchronizer.sync(trx, transaction);
            transactionRepository.save(transaction);

            auditUtilities.logCapturePayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            log.info("[CAPTURE_PAYMENT] SUCCESS - trxId={}, trxCode={}, status={}",
                    Utilities.sanitizeString(trx.getId()),
                    Utilities.sanitizeString(trx.getTrxCode()),
                    trx.getStatus());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

    private void validateCaptureRequest(TransactionInProgress trx, String initiativeId, String merchantId, String pointOfSaleId) {
        if (!Objects.equals(trx.getInitiativeId(), initiativeId)) {
            throw new InitiativeNotfoundException(
                    "The initiative with id [%s] associated to the transaction is not equal to the initiative with id [%s]"
                            .formatted(trx.getInitiativeId(), initiativeId));
        }
        if (!Objects.equals(trx.getMerchantId(), merchantId)) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(trx.getMerchantId(), merchantId));
        }
        if (!Objects.equals(trx.getPointOfSaleId(), pointOfSaleId)) {
            throw new PointOfSaleNotAllowedException(
                    "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]"
                            .formatted(trx.getPointOfSaleId(), pointOfSaleId));
        }
    }

    @Override
    public TransactionBarCodeResponse retriveVoucher(String intiativeId, String trxCode, String userId) {
        try {
            TransactionInProgress trx = repository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            auditUtilities.logRetriveVoucher(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorRetriveVoucher(intiativeId, trxCode, userId);
            throw e;
        }
    }

    private void deleteUnusedVouchers(TransactionInProgress trx) {
        List<TransactionInProgress> otherTrxs = repository
                .findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                        trx.getUserId(),
                        trx.getInitiativeId(),
                        SyncTrxStatus.CREATED,
                        trx.getExtendedAuthorization()
                );

        if (!otherTrxs.isEmpty()) {
            repository.deleteAll(otherTrxs);
            otherTrxs.forEach(otherTrx ->
                    log.info("[CAPTURE_PAYMENT] Removed unused {} voucher (id={}) for user={} initiative={}",
                            Boolean.TRUE.equals(otherTrx.getExtendedAuthorization()) ? "WEB" : "APP",
                            otherTrx.getId(),
                            trx.getUserId(),
                            trx.getInitiativeId())
            );
        }
    }

    private void deleteUnusedVouchers(Transaction trx) {
        List<Transaction> otherTrxs = transactionRepository
                .findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                        trx.getUserId(),
                        trx.getInitiativeId(),
                        SyncTrxStatus.CREATED,
                        trx.getExtendedAuthorization()
                );

        if (!otherTrxs.isEmpty()) {
            transactionRepository.deleteAll(otherTrxs);
            otherTrxs.forEach(otherTrx ->
                    log.info("[CAPTURE_PAYMENT] Removed unused {} voucher (id={}) for user={} initiative={}",
                            Boolean.TRUE.equals(otherTrx.getExtendedAuthorization()) ? "WEB" : "APP",
                            otherTrx.getId(),
                            trx.getUserId(),
                            trx.getInitiativeId())
            );
        }
    }
}