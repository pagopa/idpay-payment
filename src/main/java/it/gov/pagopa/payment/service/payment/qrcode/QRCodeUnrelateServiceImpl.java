package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

@Service
@Slf4j
public class QRCodeUnrelateServiceImpl implements QRCodeUnrelateService{

    private final TransactionRepository transactionRepository;
    private final AuditUtilities auditUtilities;

    public QRCodeUnrelateServiceImpl(
            TransactionRepository transactionRepository,
            AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void unrelateTransaction(String trxCode, String userId) {
        try {
            Transaction transaction = transactionRepository.findByTrxCodeAndTrxEndDateGreaterThanEqual(trxCode, LocalDateTime.now(ZoneId.of("Europe/Rome")))
                    .orElseThrow(() ->  new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode)));

            if (SyncTrxStatus.IDENTIFIED.equals(transaction.getStatus())) {
                if(!transaction.getUserId().equals(userId)){
                    throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED, "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
                }

                revertTrxToCreatedStatus(transaction);
                transactionRepository.save(transaction);

                log.info("[TRX_STATUS][UNRELATED] The transaction with trxId {} trxCode {}, has been cancelled", transaction.getId(), transaction.getTrxCode());
                auditUtilities.logUnrelateTransaction(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), ObjectUtils.firstNonNull(transaction.getRewardCents(), 0L), transaction.getRejectionReasons());
            } else {
                throw new OperationNotAllowedException(ExceptionCode.TRX_UNRELATE_NOT_ALLOWED, "Cannot unrelate transaction with transactionId [%s] not in status identified".formatted(transaction.getId()));
            }
        } catch (RuntimeException e) {
            auditUtilities.logErrorUnrelateTransaction(trxCode, userId);
            throw e;
        }
    }

    private static void revertTrxToCreatedStatus(Transaction transaction) {
        transaction.setStatus(SyncTrxStatus.CREATED);
        transaction.setUserId(null);
        transaction.setRewardCents(null);
        transaction.setRewards(null);
        transaction.setChannel(null);
        transaction.setRejectionReasons(Collections.emptyList());
        transaction.setUpdateDate(LocalDateTime.now(ZoneId.of("Europe/Rome")));
        transaction.setTrxChargeDate(null);
    }

}