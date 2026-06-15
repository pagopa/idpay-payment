package it.gov.pagopa.payment.service.payment.barcode;

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
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper mapper;
    private final AuditUtilities auditUtilities;

    public BarCodeCaptureServiceImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            TransactionBarCodeInProgress2TransactionResponseMapper mapper,
            AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
    }

    public TransactionBarCodeResponse capturePayment(String trxCode) {
        try {
            String code = normalize(trxCode);

            TransactionInProgress mongo = loadMongo(code);
            Transaction postgres = loadPostgres(code);

            validateAuthorized(code, mongo);
            validateAuthorized(code, postgres);

            deleteUnusedVouchersMongo(mongo.getUserId(), mongo.getInitiativeId(), mongo.getExtendedAuthorization());
            deleteUnusedVouchersPostgre(postgres.getUserId(), postgres.getInitiativeId(), postgres.getExtendedAuthorization());

            captureMongo(mongo);
            capturePostgres(postgres);

            auditUtilities.logCapturePayment(
                    mongo.getInitiativeId(),
                    mongo.getId(),
                    mongo.getTrxCode(),
                    mongo.getUserId(),
                    mongo.getRewardCents(),
                    mongo.getRejectionReasons(),
                    mongo.getMerchantId()
            );

            return mapper.apply(mongo);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

    private TransactionInProgress loadMongo(String trxCode) {
        return transactionInProgressRepository.findByTrxCode(trxCode)
                .orElseThrow(() -> notFound(trxCode));
    }

    private Transaction loadPostgres(String trxCode) {
        return transactionRepository.findByTrxCode(trxCode)
                .orElseThrow(() -> notFound(trxCode));
    }

    private TransactionNotFoundOrExpiredException notFound(String trxCode) {
        return new TransactionNotFoundOrExpiredException(
                "Cannot find transaction [%s]".formatted(trxCode)
        );
    }

    private String normalize(String trxCode) {
        return trxCode.toLowerCase();
    }

    private void validateAuthorized(String trxCode, TransactionInProgress transaction){
        if(!transaction.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
            throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,transaction.getStatus()));
        }
    }

    private void validateAuthorized(String trxCode, Transaction transaction){
        if(!transaction.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
            throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,transaction.getStatus()));
        }
    }

    private void captureMongo(TransactionInProgress mongo){
        mongo.setStatus(SyncTrxStatus.CAPTURED);
        mongo.setElaborationDateTime(LocalDateTime.now());
        mongo.setUpdateDate(LocalDateTime.now());
        transactionInProgressRepository.save(mongo);
    }

    private void capturePostgres(Transaction postgres){
        postgres.setStatus(SyncTrxStatus.CAPTURED);
        postgres.setElaborationDate(LocalDateTime.now());
        postgres.setUpdateDate(LocalDateTime.now());
        transactionRepository.save(postgres);
    }

    @Override
    public TransactionBarCodeResponse retriveVoucher(String initiativeId, String trxCode, String userId) {
        try {
            String code = normalize(trxCode);

            TransactionInProgress mongo = transactionInProgressRepository.findByInitiativeIdAndTrxCodeAndUserId(initiativeId, code, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            Transaction postgres = transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(initiativeId, code, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            auditUtilities.logRetriveVoucher(
                    mongo.getInitiativeId(),
                    mongo.getId(),
                    mongo.getTrxCode(),
                    mongo.getUserId(),
                    mongo.getRewardCents(),
                    mongo.getRejectionReasons()
            );

            return mapper.apply(mongo);
        } catch (RuntimeException e) {
            auditUtilities.logErrorRetriveVoucher(initiativeId, trxCode, userId);
            throw e;
        }
    }

    private void deleteUnusedVouchersMongo(String userId, String initiativeId, Boolean extendedAuthorization) {
        List<TransactionInProgress> otherTrxs = transactionInProgressRepository
                .findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                        userId,
                        initiativeId,
                        SyncTrxStatus.CREATED,
                        extendedAuthorization
                );

        if (!otherTrxs.isEmpty()) {
            transactionInProgressRepository.deleteAll(otherTrxs);
            otherTrxs.forEach(otherTrx ->
                    log.info("[CAPTURE_PAYMENT] Removed unused {} voucher (id={}) for user={} initiative={}",
                            Boolean.TRUE.equals(otherTrx.getExtendedAuthorization()) ? "WEB" : "APP",
                            otherTrx.getId(),
                            userId,
                            initiativeId)
            );
        }
    }

    private void deleteUnusedVouchersPostgre(String userId, String initiativeId, Boolean extendedAuthorization) {
        List<Transaction> otherTransactions = transactionRepository
                .findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                        userId,
                        initiativeId,
                        SyncTrxStatus.CREATED,
                        extendedAuthorization
                );
        if (!otherTransactions.isEmpty()) {
            transactionRepository.deleteAll(otherTransactions);
            otherTransactions.forEach(otherTrx ->
                    log.info("[CAPTURE_PAYMENT] Removed unused {} voucher (id={}) for user={} initiative={}",
                            Boolean.TRUE.equals(otherTrx.getExtendedAuthorization()) ? "WEB" : "APP",
                            otherTrx.getId(),
                            userId,
                            initiativeId)
            );
        }
    }
}
