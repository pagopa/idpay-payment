package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommonAuthServiceImpl {

    private static final String ZONE_EUROPE_ROME = "Europe/Rome";
    private final TransactionRepository transactionRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final AuditUtilities auditUtilities;
    private final WalletConnector walletConnector;
    private final CommonPreAuthServiceImpl commonPreAuthService;

    private final AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService;

    protected CommonAuthServiceImpl(
            TransactionRepository transactionRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities,
            WalletConnector walletConnector,
            @Qualifier("commonPreAuth")CommonPreAuthServiceImpl commonPreAuthService,
            AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService) {
        this.transactionRepository = transactionRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.auditUtilities = auditUtilities;
        this.walletConnector = walletConnector;
        this.commonPreAuthService = commonPreAuthService;
        this.timeoutSchedulerService = timeoutSchedulerService;
    }

    public AuthPaymentDTO previewPayment(Transaction transaction, String userId) {
        checkWalletStatus(transaction.getInitiativeId(), ObjectUtils.firstNonNull(transaction.getUserId(), userId));
        transaction.setTrxChargeDate(OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)));

        return rewardCalculatorConnector.previewTransaction(transaction);
    }

    public AuthPaymentDTO authPayment(Transaction transaction, String userId, String trxCode) {
        try {
            checkAuth(trxCode,transaction);

            checkWalletStatus(transaction.getInitiativeId(), ObjectUtils.firstNonNull(transaction.getUserId(), userId));

            checkTrxStatusToInvokePreAuth(transaction);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(transaction);

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

    public AuthPaymentDTO invokeRuleEngine(Transaction transaction) {

        AuthPaymentDTO authPaymentDTO;
        if (transaction.getStatus().equals(SyncTrxStatus.AUTHORIZATION_REQUESTED)){

            long sequenceNumber = timeoutSchedulerService.scheduleMessage(transaction.getId());
            log.info("[TRX_AUTHORIZATION] Scheduled timeout message with sequence number: {}",sequenceNumber);
            authPaymentDTO = rewardCalculatorConnector.authorizePayment(transaction);

            Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities
                    .getInitiativeRejectionReason(authPaymentDTO.getInitiativeId(), authPaymentDTO.getRejectionReasons());

            if(SyncTrxStatus.REWARDED.equals(authPaymentDTO.getStatus())) {
                log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", transaction.getId(), transaction.getTrxCode());
                transaction.setCounterVersion(authPaymentDTO.getCounters().getVersion());
                updateTrxAuthorized(transaction, authPaymentDTO, initiativeRejectionReasons);
                timeoutSchedulerService.cancelScheduledMessage(sequenceNumber);
            } else {

                transactionRepository.updateTrxRejected(transaction, SyncTrxStatus.REJECTED,  authPaymentDTO.getRejectionReasons(), initiativeRejectionReasons, LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)), "EUR");

                timeoutSchedulerService.cancelScheduledMessage(sequenceNumber);
                log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",transaction.getId(), transaction.getTrxCode());
                if (authPaymentDTO.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
                    throw new BudgetExhaustedException("Budget exhausted for the current user and initiative [%s]".formatted(transaction.getInitiativeId()));
                }
                if(authPaymentDTO.getRejectionReasons().contains(ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD)){
                    return authPaymentDTO;
                }
                throw new TransactionRejectedException("Transaction with transactionId [%s] is rejected".formatted(transaction.getId()));
            }

            transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
            transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
            transaction.setRewards(authPaymentDTO.getRewards());
            transaction.setStatus(authPaymentDTO.getStatus());
            transaction.setAdditionalProperties(authPaymentDTO.getAdditionalProperties());

        } else if (transaction.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
            throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(transaction.getId()));
        } else {
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(transaction.getId(),transaction.getStatus()));
        }
        return authPaymentDTO;
    }

    private void updateTrxAuthorized(Transaction transaction, AuthPaymentDTO authPaymentDTO, Map<String, List<String>> initiativeRejectionReasons) {

        int result = transactionRepository.updateTrxAuthorized(
                transaction,
                authPaymentDTO,
                initiativeRejectionReasons,
                SyncTrxStatus.AUTHORIZATION_REQUESTED,
                SyncTrxStatus.AUTHORIZED,
                LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)),
                "EUR"
        );

        if(result == 0){
            authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
            authPaymentDTO.setRejectionReasons(List.of(PaymentConstants.PAYMENT_AUTHORIZATION_TIMEOUT));
            authPaymentDTO.setRewards(Collections.emptyMap());
            authPaymentDTO.setRewardCents(null);
            authPaymentDTO.setCounters(null);
            authPaymentDTO.setCounterVersion(0L);
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

    public WalletDTO checkWalletStatusAndReturn(String initiativeId, String userId){
        WalletDTO walletDTO = walletConnector.getWallet(initiativeId, userId);
        String walletStatus = walletDTO.getStatus();

        if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
            throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(initiativeId));
        }

        if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
            throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(initiativeId));
        }
        return  walletDTO;
    }
    public void checkAuth(String trxCode, Transaction transaction){
        if (transaction == null) {
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
        }
        if(transaction.getStatus().equals(SyncTrxStatus.CAPTURED)){
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(transaction.getId(),transaction.getStatus()));
        }
    }

    public void checkTrxStatusToInvokePreAuth(Transaction transaction) {
        if ((transaction.getStatus().equals(SyncTrxStatus.CREATED) && transaction.getUserId() != null) ||
                (transaction.getStatus().equals(SyncTrxStatus.IDENTIFIED) && transaction.getRewardCents() == null)){
            AuthPaymentDTO preAuth = commonPreAuthService.previewPayment(transaction, transaction.getChannel(), SyncTrxStatus.AUTHORIZATION_REQUESTED);
            transaction.setStatus(preAuth.getStatus());
            transaction.setRewardCents(preAuth.getRewardCents());
            transaction.setRewards(preAuth.getRewards());
            transaction.setRejectionReasons(preAuth.getRejectionReasons());
            transaction.setCounterVersion(preAuth.getCounterVersion());
        } else if(transaction.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
            transaction.setStatus(SyncTrxStatus.AUTHORIZATION_REQUESTED);
        }
        transactionRepository.updateTrxWithStatus(transaction, LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)));
    }

    protected void logAuthorizedPayment(String initiativeId, String id, String trxCode, String userId, Long rewardCents, List<String> rejectionReasons) {
        auditUtilities.logAuthorizedPayment(initiativeId, id, trxCode, userId, rewardCents, rejectionReasons);
    }

    protected  void logErrorAuthorizedPayment(String trxCode, String userId){
        auditUtilities.logErrorAuthorizedPayment(trxCode, userId);
    }

}