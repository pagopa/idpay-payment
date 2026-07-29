package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.PointOfSaleNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class BarCodeCaptureServiceImpl implements BarCodeCaptureService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final AuditUtilities auditUtilities;
    private final MerchantConnector merchantConnector;

    public BarCodeCaptureServiceImpl(TransactionRepository transactionRepository,
                                     TransactionMapper mapper,
                                     AuditUtilities auditUtilities,
                                     MerchantConnector merchantConnector) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
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

            Transaction transaction = transactionRepository.findByTrxCodeAndStatusNot(normalizedTrxCode, SyncTrxStatus.CANCELLED)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!transaction.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,transaction.getStatus()));
            }

            validateCaptureRequest(transaction, initiativeId, merchantId, pointOfSaleId, acquirerId);

            log.info("[CAPTURE_PAYMENT] Merchant/POS onboarding checks - merchantId={}, pointOfSaleId={}, initiativeId={}",
                    Utilities.sanitizeString(merchantId),
                    Utilities.sanitizeString(pointOfSaleId),
                    Utilities.sanitizeString(initiativeId));
            merchantConnector.merchantDetail(merchantId, initiativeId);
            merchantConnector.getPointOfSale(merchantId, pointOfSaleId, initiativeId);
            log.info("[CAPTURE_PAYMENT] Checks Passed");

            deleteUnusedVouchers(transaction);

            transaction.setStatus(SyncTrxStatus.CAPTURED);
            transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")));
            transaction.setUpdateDate(LocalDateTime.now(ZoneId.of("Europe/Rome")));
            transactionRepository.save(transaction);

            auditUtilities.logCapturePayment(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), transaction.getRewardCents(), transaction.getRejectionReasons(), transaction.getMerchantId());
            log.info("[CAPTURE_PAYMENT] SUCCESS - trxId={}, trxCode={}, status={}",
                    Utilities.sanitizeString(transaction.getId()),
                    Utilities.sanitizeString(transaction.getTrxCode()),
                    transaction.getStatus());
            return mapper.transactionBarCodeToTransactionResponse(transaction);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

    private void validateCaptureRequest(Transaction transaction, String initiativeId, String merchantId, String pointOfSaleId, String acquirerId) {
        if (!Objects.equals(transaction.getInitiativeId(), initiativeId)) {
            throw new InitiativeNotfoundException(
                    "The initiative with id [%s] associated to the transaction is not equal to the initiative with id [%s]"
                            .formatted(transaction.getInitiativeId(), initiativeId));
        }
        if (!Objects.equals(transaction.getMerchantId(), merchantId) || !Objects.equals(transaction.getAcquirerId(), acquirerId)) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(transaction.getMerchantId(), merchantId));
        }
        if (!Objects.equals(transaction.getPointOfSaleId(), pointOfSaleId)) {
            throw new PointOfSaleNotAllowedException(
                    "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]"
                            .formatted(transaction.getPointOfSaleId(), pointOfSaleId));
        }
    }

    @Override
    public TransactionBarCodeResponse retriveVoucher(String intiativeId, String trxCode, String userId) {
        try {

            Transaction transaction = transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            auditUtilities.logRetriveVoucher(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), transaction.getRewardCents(), transaction.getRejectionReasons());

            return mapper.transactionBarCodeToTransactionResponse(transaction);
        } catch (RuntimeException e) {
            auditUtilities.logErrorRetriveVoucher(intiativeId, trxCode, userId);
            throw e;
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