package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class QRCodeUnrelateServiceImpl implements QRCodeUnrelateService{

    private final TransactionInProgressRepository repository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final AuditUtilities auditUtilities;

    public QRCodeUnrelateServiceImpl(TransactionInProgressRepository repository, RewardCalculatorConnector rewardCalculatorConnector, AuditUtilities auditUtilities) {
        this.repository = repository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void unrelateTransaction(String trxCode, String userId) {
        try {
            TransactionInProgress trx = repository.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());

            if (trx == null) {
                throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "[UNRELATE_TRANSACTION] Cannot find transaction having code: %s".formatted(trxCode));
            }

            String trxId = trx.getId();
            if(!trx.getUserId().equals(userId)){
                throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "[UNRELATE_TRANSACTION] Requesting userId (%s) not allowed to operate on transaction having id %s".formatted(userId, trxId));
            }
            if(List.of(SyncTrxStatus.AUTHORIZED, SyncTrxStatus.REWARDED).contains(trx.getStatus())){
                throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "[UNRELATE_TRANSACTION] Cannot unrelate already authorized transaction: id %s".formatted(trxId));
            }

            if (SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())) {
                AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(trx);
                if(refund!=null) {
                    trx.setReward(refund.getReward());
                    trx.setRewards(refund.getRewards());
                }

                trx.setStatus(SyncTrxStatus.CREATED);
                trx.setUserId(null);
                trx.setElaborationDateTime(LocalDateTime.now());

                // TODO sendCancelledTransactionNotification(trx); ?

                repository.save(trx);
            }

            log.info("[TRX_STATUS][UNRELATED] The transaction with trxId {} trxCode {}, has been cancelled", trx.getId(), trx.getTrxCode());

            auditUtilities.logUnrelateTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), ObjectUtils.firstNonNull(trx.getReward(), 0L), trx.getRejectionReasons());
        } catch (RuntimeException e) {
            auditUtilities.logErrorUnrelateTransaction(trxCode, userId);
            throw e;
        }
    }
}
