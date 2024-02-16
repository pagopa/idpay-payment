package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeUnrelateServiceImpl implements QRCodeUnrelateService{

    private final TransactionInProgressRepository repository;
    private final QRCodeAuthorizationExpiredService authorizationExpiredService;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final AuditUtilities auditUtilities;

    public QRCodeUnrelateServiceImpl(
            TransactionInProgressRepository repository,
            QRCodeAuthorizationExpiredService authorizationExpiredService,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        this.repository = repository;
        this.authorizationExpiredService = authorizationExpiredService;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void unrelateTransaction(String trxCode, String userId) {
        try {
            TransactionInProgress trx = authorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());

            if (trx == null) {
                throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
            }

            if (SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())) {
                if(!trx.getUserId().equals(userId)){
                    throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED, "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
                }

                callRewardCalculatorCancelTransaction(trx);

                revertTrxToCreatedStatus(trx);
                repository.save(trx);

                log.info("[TRX_STATUS][UNRELATED] The transaction with trxId {} trxCode {}, has been cancelled", trx.getId(), trx.getTrxCode());
                auditUtilities.logUnrelateTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), ObjectUtils.firstNonNull(trx.getReward(), 0L), trx.getRejectionReasons());
            } else {
                throw new OperationNotAllowedException(ExceptionCode.TRX_UNRELATE_NOT_ALLOWED, "Cannot unrelate transaction with transactionId [%s] not in status identified".formatted(trx.getId()));
            }
        } catch (RuntimeException e) {
            auditUtilities.logErrorUnrelateTransaction(trxCode, userId);
            throw e;
        }
    }

    private static void revertTrxToCreatedStatus(TransactionInProgress trx) {
        trx.setStatus(SyncTrxStatus.CREATED);
        trx.setUserId(null);
        trx.setReward(null);
        trx.setRewards(null);
        trx.setChannel(null);
        trx.setRejectionReasons(Collections.emptyList());
        trx.setUpdateDate(LocalDateTime.now());
        trx.setTrxChargeDate(null);
    }

    private void callRewardCalculatorCancelTransaction(TransactionInProgress trx) {
        try {
            rewardCalculatorConnector.cancelTransaction(trx);
        } catch (TransactionNotFoundOrExpiredException e) {
            // do nothing
        }
    }
}
