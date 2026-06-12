package it.gov.pagopa.payment.service.payment.common;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommonAuthServiceImpl {
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final AuditUtilities auditUtilities;
    private final WalletConnector walletConnector;
    private final CommonPreAuthServiceImpl commonPreAuthService;

    private final AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService;

    protected CommonAuthServiceImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities,
            WalletConnector walletConnector,
            @Qualifier("commonPreAuth")CommonPreAuthServiceImpl commonPreAuthService,
            AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.auditUtilities = auditUtilities;
        this.walletConnector = walletConnector;
        this.commonPreAuthService = commonPreAuthService;
        this.timeoutSchedulerService = timeoutSchedulerService;
    }

    public AuthPaymentDTO previewPayment(Transaction transaction, String userId) {
        checkWalletStatus(transaction.getInitiativeId(), ObjectUtils.firstNonNull(transaction.getUserId(), userId));
        transaction.setTrxChargeDate(OffsetDateTime.now());
        return rewardCalculatorConnector.previewTransaction(transaction);
    }

    public AuthPaymentDTO previewPayment(TransactionInProgress trx, String userId) {
        checkWalletStatus(trx.getInitiativeId(), ObjectUtils.firstNonNull(trx.getUserId(), userId));
        trx.setTrxChargeDate(OffsetDateTime.now());

        return rewardCalculatorConnector.previewTransaction(trx);
    }

    public AuthPaymentDTO authPayment(Transaction transaction, TransactionInProgress trx, String userId, String trxCode) {
        try {
            checkAuth(trxCode, transaction);
            checkAuth(trxCode,trx);

            checkWalletStatus(trx.getInitiativeId(), ObjectUtils.firstNonNull(trx.getUserId(), userId));

            checkTrxStatusToInvokePreAuth(trx);
            checkTrxStatusToInvokePreAuth(transaction);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(transaction, trx);

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

    public AuthPaymentDTO invokeRuleEngine(Transaction transaction, TransactionInProgress trx) {

        AuthPaymentDTO authPaymentDTO;
        if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZATION_REQUESTED) && transaction.getStatus().equals(SyncTrxStatus.AUTHORIZATION_REQUESTED)){

            long sequenceNumber = timeoutSchedulerService.scheduleMessage(trx.getId());
            log.info("[TRX_AUTHORIZATION] Scheduled timeout message with sequence number: {}",sequenceNumber);
            authPaymentDTO = rewardCalculatorConnector.authorizePayment(trx);
            //Successivamente sostituire con transaction non transaction in progress

            Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities
                    .getInitiativeRejectionReason(authPaymentDTO.getInitiativeId(), authPaymentDTO.getRejectionReasons());

            if(SyncTrxStatus.REWARDED.equals(authPaymentDTO.getStatus())) {
                log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", trx.getId(), trx.getTrxCode());
                trx.setCounterVersion(authPaymentDTO.getCounters().getVersion());
                updateTrxAuthorized(transaction, trx, authPaymentDTO, initiativeRejectionReasons);
                timeoutSchedulerService.cancelScheduledMessage(sequenceNumber);
            } else {
                updateTrxRejected(transaction, trx, authPaymentDTO, initiativeRejectionReasons);
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

            transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
            transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
            transaction.setRewards(authPaymentDTO.getRewards());
            transaction.setStatus(authPaymentDTO.getStatus());
            transaction.setAdditionalProperties(authPaymentDTO.getAdditionalProperties());

            trx.setRejectionReasons(authPaymentDTO.getRejectionReasons());
            trx.setInitiativeRejectionReasons(initiativeRejectionReasons);
            trx.setRewards(authPaymentDTO.getRewards());
            trx.setStatus(authPaymentDTO.getStatus());
            trx.setAdditionalProperties(authPaymentDTO.getAdditionalProperties());

        } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
            throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(trx.getId()));
        } else {
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trx.getId(),trx.getStatus()));
        }
        return authPaymentDTO;
    }

    private void updateTrxRejected(Transaction transaction, TransactionInProgress trx, AuthPaymentDTO authPaymentDTO, Map<String, List<String>> initiativeRejectionReasons){
        transaction.setStatus(SyncTrxStatus.REJECTED);
        transaction.setRewardCents(0L);
        transaction.setRewards(Collections.emptyMap());
        transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
        transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
        transaction.setTrxChargeDate(trx.getTrxChargeDate());
        transaction.setUpdateDate(LocalDateTime.now());
        if (RewardConstants.TRX_CHANNEL_BARCODE.equals(trx.getChannel())) {
            trx.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
        }
        transactionRepository.save(transaction);

        transactionInProgressRepository.updateTrxRejected(trx, authPaymentDTO.getRejectionReasons(), initiativeRejectionReasons);
    }

    private void updateTrxAuthorized(Transaction transaction, TransactionInProgress trx, AuthPaymentDTO authPaymentDTO, Map<String, List<String>> initiativeRejectionReasons) {
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);
        transaction.setRewardCents(authPaymentDTO.getRewardCents());
        transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
        transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
        transaction.setRewards(authPaymentDTO.getRewards());
        transaction.setTrxChargeDate(trx.getTrxEndDate());
        transaction.setCounterVersion(authPaymentDTO.getCounters().getVersion());
        transaction.setFamilyId(trx.getFamilyId());
        transaction.setUpdateDate(LocalDateTime.now());

        if (RewardConstants.TRX_CHANNEL_BARCODE.equals(trx.getChannel())) {
            transaction.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
            transaction.setAmountCents(trx.getAmountCents());
            transaction.setEffectiveAmountCents(trx.getEffectiveAmountCents());
            transaction.setIdTrxAcquirer(trx.getIdTrxAcquirer());
            transaction.setMerchantId(trx.getMerchantId());
            transaction.setBusinessName(trx.getBusinessName());
            transaction.setVat(trx.getVat());
            transaction.setMerchantFiscalCode(trx.getMerchantFiscalCode());
            transaction.setAcquirerId(trx.getAcquirerId());
            transaction.setFamilyId(trx.getFamilyId());
        }
        int updatedRows = transactionRepository.updateAuthorized(
                transaction.getId(),
                SyncTrxStatus.AUTHORIZATION_REQUESTED,
                SyncTrxStatus.AUTHORIZED,
                authPaymentDTO.getRewardCents(),
                authPaymentDTO.getRejectionReasons(),
                initiativeRejectionReasons,
                authPaymentDTO.getRewards(),
                transaction.getTrxChargeDate(),
                authPaymentDTO.getCounters().getVersion(),
                transaction.getFamilyId(),
                transaction.getUpdateDate()
        );

        UpdateResult result = transactionInProgressRepository.updateTrxAuthorized(trx,
                authPaymentDTO,
                initiativeRejectionReasons);
        if(result.getModifiedCount() == 0 && updatedRows == 0){
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

    public void checkAuth(String trxCode, TransactionInProgress trx){
        if (trx == null) {
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
        }
        if(trx.getStatus().equals(SyncTrxStatus.CAPTURED)){
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trx.getId(),trx.getStatus()));
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
        transactionRepository.save(transaction);
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
