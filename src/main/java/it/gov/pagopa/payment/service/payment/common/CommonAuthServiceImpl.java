package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionRejectedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserSuspendedException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.servererror.InternalServerErrorException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CommonAuthServiceImpl {

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    protected final AuditUtilities auditUtilities;
    private final WalletConnector walletConnector;

    protected CommonAuthServiceImpl(
            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
            AuditUtilities auditUtilities,
            WalletConnector walletConnector) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
        this.walletConnector = walletConnector;
    }

    protected AuthPaymentDTO authPayment(TransactionInProgress trx, String userId, String trxCode) {
        try {
            checkAuth(trxCode,trx);

            checkWalletStatus(trx.getInitiativeId(), trx.getUserId() != null ? trx.getUserId() : userId);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, userId, authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
            authPaymentDTO.setResidualBudget(CommonUtilities.calculateResidualBudget(trx.getRewards()));
            authPaymentDTO.setRejectionReasons(null);
            return authPaymentDTO;
        } catch (RuntimeException e) {
            logErrorAuthorizedPayment(trxCode, userId);
            throw e;
        }
    }

    protected AuthPaymentDTO invokeRuleEngine(TransactionInProgress trx){
        AuthPaymentDTO authPaymentDTO;
        if (trx.getStatus().equals(getSyncTrxStatus())) {
            trx.setTrxChargeDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
            authPaymentDTO = rewardCalculatorConnector.authorizePayment(trx);

            trx.setReward(authPaymentDTO.getReward());
            trx.setRewards(authPaymentDTO.getRewards());
            trx.setRejectionReasons(authPaymentDTO.getRejectionReasons());

            if(SyncTrxStatus.REWARDED.equals(authPaymentDTO.getStatus())) {
                log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", trx.getId(), trx.getTrxCode());
                authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);
                transactionInProgressRepository.updateTrxAuthorized(trx,
                        authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
            } else {
                transactionInProgressRepository.updateTrxRejected(trx, authPaymentDTO.getRejectionReasons());
                log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
                if (authPaymentDTO.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
                    throw new BudgetExhaustedException("Budget exhausted for the current user and initiative [%s]".formatted(trx.getInitiativeId()));
                }
                throw new TransactionRejectedException("Transaction with transactionId [%s] is rejected".formatted(trx.getId()));
            }

            trx.setStatus(authPaymentDTO.getStatus());

            sendAuthPaymentNotification(trx);

        } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
            throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(trx.getId()));
        } else {
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trx.getId(),trx.getStatus()));
        }
        return authPaymentDTO;
    }

    private void sendAuthPaymentNotification(TransactionInProgress trx) {
        try {
            log.info("[AUTHORIZE_TRANSACTION][SEND_NOTIFICATION] Sending Authorization Payment event to Notification: trxId {} - userId {}", trx.getId(), trx.getUserId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while Auth Payment notify");
            }
        } catch (Exception e) {
            if(!paymentErrorNotifierService.notifyAuthPayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result: trxId %s - userId %s".formatted(trx.getId(), trx.getUserId()),
                    true,
                    e)
            ) {
                log.error("[AUTHORIZE_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - userId {}", trx.getId(), trx.getUserId(), e);
            }
        }
    }

    protected void checkWalletStatus(String initiativeId, String userId){
        String walletStatus = walletConnector.getWallet(initiativeId, userId).getStatus();

        if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
            throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(initiativeId));
        }

        if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
            throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(initiativeId));
        }
    }

    protected void checkAuth(String trxCode, TransactionInProgress trx){
        if (trx == null) {
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
        }

    }

    protected void logAuthorizedPayment(String initiativeId, String id, String trxCode, String userId, Long reward, List<String> rejectionReasons) {
        auditUtilities.logAuthorizedPayment(initiativeId, id, trxCode, userId, reward, rejectionReasons);
    }

    protected  void logErrorAuthorizedPayment(String trxCode, String userId){
        auditUtilities.logErrorAuthorizedPayment(trxCode, userId);
    }

    protected SyncTrxStatus getSyncTrxStatus(){
        return SyncTrxStatus.IDENTIFIED;
    }
}
