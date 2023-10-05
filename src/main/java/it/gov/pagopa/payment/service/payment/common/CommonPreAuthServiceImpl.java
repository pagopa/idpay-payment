package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

@Slf4j
public class CommonPreAuthServiceImpl{
  private final long authorizationExpirationMinutes;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuditUtilities auditUtilities;
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
      String walletStatus = walletConnector.getWallet(trx.getInitiativeId(), userId).getStatus();

      checkPreAuth(trx.getTrxCode(), userId, trx, walletStatus);

      trx.setUserId(userId);
      trx.setTrxChargeDate(OffsetDateTime.now());

      return trx;

    } catch (ClientException e) {
      auditUtilities.logErrorRelatedUserToTransaction(trx.getTrxCode(), userId);
      throw e;
    }
  }

  public AuthPaymentDTO previewPayment(TransactionInProgress trx, String channel) {
    try {
    AuthPaymentDTO preview = rewardCalculatorConnector.previewTransaction(trx);

    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
      transactionInProgressRepository.updateTrxRejected(
              trx.getId(), trx.getUserId(), preview.getRejectionReasons());
      log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
      if (preview.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
        throw new ClientExceptionWithBody(
                HttpStatus.FORBIDDEN,
                PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED,
                "Budget exhausted for user [%s] and initiative [%s]".formatted(trx.getUserId(), trx.getInitiativeId()));
      }
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.REJECTED,
              "Transaction with trxCode [%s] is rejected".formatted(trx.getTrxCode()));
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
      auditUtilities.logErrorPreviewTransaction(trx.getTrxCode(), trx.getUserId());
      throw e;
    }
  }

  private void checkPreAuth(String trxCode, String userId, TransactionInProgress trx, String walletStatus) {
    if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR,
              "User %s has been suspended for initiative %s".formatted(userId, trx.getInitiativeId()));
    }

    if (trx.getTrxDate().plusMinutes(authorizationExpirationMinutes).isBefore(OffsetDateTime.now())) {
      throw new ClientExceptionWithBody(
              HttpStatus.NOT_FOUND,
              PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
              "Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }

    if (trx.getUserId() != null && !userId.equals(trx.getUserId())) {
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ANOTHER_USER,
              "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }

    if(SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED,
              "Transaction with trxCode [%s] is already authorized".formatted(trxCode));
    }

    if(!SyncTrxStatus.CREATED.equals(trx.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())){
      throw new ClientExceptionWithBody(
              HttpStatus.BAD_REQUEST,
              PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID,
              "Cannot relate transaction [%s] in status %s".formatted(trxCode, trx.getStatus()));
    }
  }

  protected void auditLogRelateUser(TransactionInProgress trx, String channel){
    auditUtilities.logRelatedUserToTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), channel);
  }
}
