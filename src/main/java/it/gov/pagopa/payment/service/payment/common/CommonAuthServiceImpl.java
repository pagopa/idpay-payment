package it.gov.pagopa.payment.service.payment.common;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommonAuthServiceImpl {
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final AuditUtilities auditUtilities;
    private final WalletConnector walletConnector;
    private final CommonPreAuthServiceImpl commonPreAuthService;

    private final AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService;

    protected CommonAuthServiceImpl(
            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities,
            WalletConnector walletConnector,
            @Qualifier("commonPreAuth")CommonPreAuthServiceImpl commonPreAuthService,
            AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.auditUtilities = auditUtilities;
        this.walletConnector = walletConnector;
        this.commonPreAuthService = commonPreAuthService;
        this.timeoutSchedulerService = timeoutSchedulerService;
    }

    public AuthPaymentDTO authPayment(TransactionInProgress trx, String userId, String trxCode) {
        try {
            checkAuth(trxCode,trx);

            checkWalletStatus(trx.getInitiativeId(), ObjectUtils.firstNonNull(trx.getUserId(), userId));

            checkTrxStatusToInvokePreAuth(trx);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, userId, authPaymentDTO.getRewardCents(), authPaymentDTO.getRejectionReasons());
            if(authPaymentDTO.getRejectionReasons() == null || authPaymentDTO.getRejectionReasons().isEmpty()) {
                authPaymentDTO.setResidualBudgetCents(CommonPaymentUtilities.calculateResidualBudget(authPaymentDTO.getRewards()));
                authPaymentDTO.setRejectionReasons(Collections.emptyList());
            }
            return authPaymentDTO;
        } catch (RuntimeException e) {
            logErrorAuthorizedPayment(trxCode, userId);
            throw e;
        }
    }

    public AuthPaymentDTO invokeRuleEngine(TransactionInProgress trx) {

        AuthPaymentDTO authPaymentDTO;
        if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZATION_REQUESTED)){

            long sequenceNumber = timeoutSchedulerService.scheduleMessage(trx.getId());
            log.info("[TRX_AUTHORIZATION] Scheduled timeout message with sequence number: {}",sequenceNumber);
            authPaymentDTO = rewardCalculatorConnector.authorizePayment(trx);

            Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities
                    .getInitiativeRejectionReason(authPaymentDTO.getInitiativeId(), authPaymentDTO.getRejectionReasons());

            if(SyncTrxStatus.REWARDED.equals(authPaymentDTO.getStatus())) {
                log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", trx.getId(), trx.getTrxCode());
                trx.setCounterVersion(authPaymentDTO.getCounters().getVersion());
                updateTrxAuthorized(trx, authPaymentDTO, initiativeRejectionReasons);
                timeoutSchedulerService.cancelScheduledMessage(sequenceNumber);
            } else {
                transactionInProgressRepository.updateTrxRejected(trx, authPaymentDTO.getRejectionReasons(), initiativeRejectionReasons);
                timeoutSchedulerService.cancelScheduledMessage(sequenceNumber);
                log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
                if (authPaymentDTO.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
                    throw new BudgetExhaustedException("Budget exhausted for the current user and initiative [%s]".formatted(trx.getInitiativeId()));
                }
                if(authPaymentDTO.getRejectionReasons().contains(ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD)){
                    return authPaymentDTO;
                }
                throw new TransactionRejectedException("Transaction with transactionId [%s] is rejected".formatted(trx.getId()));
            }

            trx.setRejectionReasons(authPaymentDTO.getRejectionReasons());
            trx.setInitiativeRejectionReasons(initiativeRejectionReasons);
            trx.setRewards(authPaymentDTO.getRewards());
            trx.setStatus(authPaymentDTO.getStatus());

        } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
            throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(trx.getId()));
        } else {
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trx.getId(),trx.getStatus()));
        }
        return authPaymentDTO;
    }

    private void updateTrxAuthorized(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO, Map<String, List<String>> initiativeRejectionReasons) {
        UpdateResult result = transactionInProgressRepository.updateTrxAuthorized(trx,
                authPaymentDTO,
                initiativeRejectionReasons);
        if(result.getModifiedCount() == 0){
            authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
            authPaymentDTO.setRejectionReasons(List.of(PaymentConstants.PAYMENT_AUTHORIZATION_TIMEOUT));
            authPaymentDTO.setRewards(Collections.emptyMap());
            authPaymentDTO.setRewardCents(null);
            authPaymentDTO.setCounters(null);
            authPaymentDTO.setCounterVersion(0);
        } else {
            authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);
            authPaymentDTO.setCounterVersion(authPaymentDTO.getCounters().getVersion());
        }
    }

    public void checkWalletStatus(String initiativeId, String userId){
        String walletStatus = walletConnector.getWallet(initiativeId, userId).getStatus();

        if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
            throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(initiativeId));
        }

        if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
            throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(initiativeId));
        }
    }

    public void checkAuth(String trxCode, TransactionInProgress trx){
        if (trx == null) {
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
        }

    }

    public void checkTrxStatusToInvokePreAuth(TransactionInProgress trx) {
        if ((trx.getStatus().equals(SyncTrxStatus.CREATED) && trx.getUserId() != null) ||
                (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED) && trx.getRewardCents() == null)){
            AuthPaymentDTO preAuth = commonPreAuthService.previewPayment(trx, trx.getChannel(), SyncTrxStatus.AUTHORIZATION_REQUESTED);
            trx.setStatus(preAuth.getStatus());
            trx.setRewardCents(preAuth.getRewardCents());
            trx.setRewards(preAuth.getRewards());
            trx.setRejectionReasons(preAuth.getRejectionReasons());
            trx.setCounterVersion(preAuth.getCounterVersion());
        } else if(trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
            trx.setStatus(SyncTrxStatus.AUTHORIZATION_REQUESTED);
        }
        transactionInProgressRepository.updateTrxWithStatus(trx);
    }

    protected void logAuthorizedPayment(String initiativeId, String id, String trxCode, String userId, Long rewardCents, List<String> rejectionReasons) {
        auditUtilities.logAuthorizedPayment(initiativeId, id, trxCode, userId, rewardCents, rejectionReasons);
    }

    protected  void logErrorAuthorizedPayment(String trxCode, String userId){
        auditUtilities.logErrorAuthorizedPayment(trxCode, userId);
    }

}
