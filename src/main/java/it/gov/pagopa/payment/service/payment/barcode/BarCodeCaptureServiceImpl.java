package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class BarCodeCaptureServiceImpl implements BarCodeCaptureService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final AuditUtilities auditUtilities;

    public BarCodeCaptureServiceImpl(TransactionRepository transactionRepository,
                                     TransactionMapper mapper,
                                     AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
    }

    public TransactionBarCodeResponse capturePayment(String trxCode) {
        try {

            Transaction transaction = transactionRepository.findByTrxCodeAndStatusNot(trxCode.toLowerCase(), SyncTrxStatus.CANCELLED)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!transaction.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,transaction.getStatus()));
            }

            deleteUnusedVouchers(transaction);
            deleteUnusedVouchers(transaction);

            transaction.setStatus(SyncTrxStatus.CAPTURED);
            transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")));
            transaction.setUpdateDate(LocalDateTime.now(ZoneId.of("Europe/Rome")));
            transactionRepository.save(transaction);

            auditUtilities.logCapturePayment(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), transaction.getRewardCents(), transaction.getRejectionReasons(), transaction.getMerchantId());
            return mapper.transactionBarCodeToTransactionResponse(transaction);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
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