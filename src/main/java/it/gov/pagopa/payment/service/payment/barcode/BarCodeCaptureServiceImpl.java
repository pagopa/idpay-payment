package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BarCodeCaptureServiceImpl implements BarCodeCaptureService {

    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository repository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper mapper;
    private final AuditUtilities auditUtilities;
    private final TransactionSynchronizer transactionSynchronizer;

    public BarCodeCaptureServiceImpl(TransactionRepository transactionRepository,
                                     TransactionInProgressRepository repository,
                                     TransactionBarCodeInProgress2TransactionResponseMapper mapper,
                                     AuditUtilities auditUtilities,
                                     TransactionSynchronizer transactionSynchronizer) {
        this.transactionRepository = transactionRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
        this.transactionSynchronizer = transactionSynchronizer;
    }

    public TransactionBarCodeResponse capturePayment(String trxCode) {
        try {
            TransactionInProgress trx = repository.findByTrxCode(trxCode.toLowerCase())
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            Transaction transaction = transactionRepository.findByTrxCode(trxCode.toLowerCase())
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,trx.getStatus()));
            }

            deleteUnusedVouchers(trx);
            deleteUnusedVouchers(transaction);

            trx.setStatus(SyncTrxStatus.CAPTURED);
            trx.setElaborationDateTime(LocalDateTime.now());
            trx.setUpdateDate(LocalDateTime.now());
            repository.save(trx);

            transactionSynchronizer.sync(trx, transaction);
            transactionRepository.save(transaction);

            auditUtilities.logCapturePayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

    @Override
    public TransactionBarCodeResponse retriveVoucher(String intiativeId, String trxCode, String userId) {
        try {
            TransactionInProgress trx = repository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            Transaction transaction = transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
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