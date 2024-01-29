package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Collections;

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
      checkPreAuth(userId, trx);

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
    trx.setChannel(channel);
    AuthPaymentDTO preview = rewardCalculatorConnector.previewTransaction(trx);

    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
      transactionInProgressRepository.updateTrxRejected(
              trx.getId(),
              trx.getUserId(),
              preview.getRejectionReasons(),
              CommonPaymentUtilities.getInitiativeRejectionReason(trx.getInitiativeId(), preview.getRejectionReasons()),
              channel);
      log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
      if (preview.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
        throw new BudgetExhaustedException("Budget exhausted for the current user and initiative [%s]".formatted(trx.getInitiativeId()));
      }
      throw new TransactionRejectedException("Transaction with transactionId [%s] is rejected".formatted(trx.getId()));
    } else {
      preview.setRejectionReasons(Collections.emptyList());
      preview.setStatus(SyncTrxStatus.IDENTIFIED);
      transactionInProgressRepository.updateTrxIdentified(trx.getId(),
              trx.getUserId(),
              preview.getReward(),
              preview.getRejectionReasons(),
              CommonPaymentUtilities.getInitiativeRejectionReason(trx.getInitiativeId(), preview.getRejectionReasons()),
              preview.getRewards(),
              channel);
    }

    Long residualBudget = CommonPaymentUtilities.calculateResidualBudget(preview.getRewards()) != null ?
            Long.sum(CommonPaymentUtilities.calculateResidualBudget(preview.getRewards()), preview.getReward()) : null;
    preview.setResidualBudget(residualBudget);

    return preview;
    } catch (RuntimeException e) {
      auditUtilities.logErrorPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), channel);
      throw e;
    }
  }

  protected void checkPreAuth(String userId, TransactionInProgress trx) {
    String walletStatus = walletConnector.getWallet(trx.getInitiativeId(), userId).getStatus();
    if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
      throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(trx.getInitiativeId()));
    }

    if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
      throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(trx.getInitiativeId()));
    }

    if (trx.getTrxDate().plusMinutes(authorizationExpirationMinutes).isBefore(OffsetDateTime.now())) {
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trx.getId()));
    }

    if (trx.getUserId() != null && !userId.equals(trx.getUserId())) {
      throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED, "Transaction with transactionId [%s] is already assigned to another user".formatted(trx.getId()));
    }

    if(SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
      throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(trx.getId()));
    }

    if(!SyncTrxStatus.CREATED.equals(trx.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())){
      throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trx.getId(), trx.getStatus()));
    }
  }

  protected void auditLogRelateUser(TransactionInProgress trx, String channel){
    auditUtilities.logRelatedUserToTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), channel);
  }
}
