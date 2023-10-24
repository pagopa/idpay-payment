package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionRejectedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotAllowedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserSuspendedException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonPreAuthServiceImpl{
  private final long authorizationExpirationMinutes;
  protected final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  protected final AuditUtilities auditUtilities;
  private final WalletConnector walletConnector;

  public CommonPreAuthServiceImpl(
          long authorizationExpirationMinutes,
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuditUtilities auditUtilities,
          WalletConnector walletConnector) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.auditUtilities = auditUtilities;
    this.walletConnector = walletConnector;
  }

  public TransactionInProgress relateUser(TransactionInProgress trx, String userId) {
    try {
      checkPreAuth(trx.getTrxCode(), userId, trx);

      trx.setUserId(userId);

      return trx;

    } catch (ServiceException e) {
      auditUtilities.logErrorRelatedUserToTransaction(trx.getTrxCode(), userId);
      throw e;
    }
  }

  public AuthPaymentDTO previewPayment(TransactionInProgress trx, String channel) {
    try {
    trx.setTrxChargeDate(OffsetDateTime.now());
    AuthPaymentDTO preview = rewardCalculatorConnector.previewTransaction(trx);

    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
      transactionInProgressRepository.updateTrxRejected(
              trx.getId(), trx.getUserId(), preview.getRejectionReasons(), channel);
      log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
      if (preview.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
        throw new BudgetExhaustedException("Budget exhausted for user [%s] and initiative [%s]".formatted(trx.getUserId(), trx.getInitiativeId()));
      }
      throw new TransactionRejectedException("Transaction with trxCode [%s] is rejected".formatted(trx.getTrxCode()));
    } else {
      preview.setRejectionReasons(null);
      preview.setStatus(SyncTrxStatus.IDENTIFIED);
      transactionInProgressRepository.updateTrxIdentified(trx.getId(), trx.getUserId(), preview.getReward(), preview.getRejectionReasons(), preview.getRewards(), channel);
    }

    Long residualBudget = CommonUtilities.calculateResidualBudget(preview.getRewards()) != null ?
            Long.sum(CommonUtilities.calculateResidualBudget(preview.getRewards()), preview.getReward()) : null;
    preview.setResidualBudget(residualBudget);

    return preview;
    } catch (RuntimeException e) {
      auditUtilities.logErrorPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), channel);
      throw e;
    }
  }

  protected void checkPreAuth(String trxCode, String userId, TransactionInProgress trx) {
    String walletStatus = walletConnector.getWallet(trx.getInitiativeId(), userId).getStatus();
    if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
      throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(trx.getInitiativeId()));
    }

    if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
      throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(trx.getInitiativeId()));
    }

    if (trx.getTrxDate().plusMinutes(authorizationExpirationMinutes).isBefore(OffsetDateTime.now())) {
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }

    if (trx.getUserId() != null && !userId.equals(trx.getUserId())) {
      throw new UserNotAllowedException("Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }

    if(SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
      throw new TransactionAlreadyAuthorizedException("Transaction with trxCode [%s] is already authorized".formatted(trxCode));
    }

    if(!SyncTrxStatus.CREATED.equals(trx.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())){
      throw new OperationNotAllowedException("Cannot relate transaction [%s] in status %s".formatted(trxCode, trx.getStatus()));
    }
  }

  protected void auditLogRelateUser(TransactionInProgress trx, String channel){
    auditUtilities.logRelatedUserToTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), channel);
  }
}
