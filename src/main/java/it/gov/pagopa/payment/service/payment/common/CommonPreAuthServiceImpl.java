package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;

@Slf4j
@Service("commonPreAuth")
public class CommonPreAuthServiceImpl{

  private static final String ZONE_EUROPE_ROME = "Europe/Rome";
  private final long authorizationExpirationMinutes;
  protected final TransactionRepository transactionRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  protected final AuditUtilities auditUtilities;
  private final WalletConnector walletConnector;

  public CommonPreAuthServiceImpl(
          @Value("${app.common.expirations.authorizationMinutes}") long authorizationExpirationMinutes,
          TransactionRepository transactionRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuditUtilities auditUtilities,
          WalletConnector walletConnector) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    this.transactionRepository = transactionRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.auditUtilities = auditUtilities;
    this.walletConnector = walletConnector;
  }

  public Transaction relateUser(Transaction transaction, String userId) {
    try {
      checkPreAuth(userId, transaction);

      transaction.setUserId(userId);

      return transaction;

    } catch (ServiceException e) {
      auditUtilities.logErrorRelatedUserToTransaction(transaction.getTrxCode(), userId);
      throw e;
    }
  }

  public AuthPaymentDTO previewPayment(Transaction transaction, String channel, SyncTrxStatus status) {
    try {
      transaction.setTrxChargeDate(OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)));
      transaction.setChannel(channel);
      AuthPaymentDTO preview = rewardCalculatorConnector.previewTransaction(transaction);

      if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {

        transactionRepository.updateTrxRejected(
                transaction.getId(),
                transaction.getUserId(),
                SyncTrxStatus.REJECTED,
                preview.getRejectionReasons(),
                CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), preview.getRejectionReasons()),
                channel,
                LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME))
        );

        log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",transaction.getId(), transaction.getTrxCode());
        if (preview.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
          throw new BudgetExhaustedException("Budget exhausted for the current user and initiative [%s]".formatted(transaction.getInitiativeId()));
        }
        throw new TransactionRejectedException("Transaction with transactionId [%s] is rejected".formatted(transaction.getId()));
      } else {
        preview.setRejectionReasons(Collections.emptyList());
        preview.setStatus(status);

        transactionRepository.updateTrxWithStatusForPreview(transaction,
                preview,
                CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), preview.getRejectionReasons()),
                channel,
                status,
                LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)));
      }

      Long residualBudget = CommonPaymentUtilities.calculateResidualBudget(preview.getRewards()) != null ?
              Long.sum(CommonPaymentUtilities.calculateResidualBudget(preview.getRewards()), preview.getRewardCents()) : null;
      preview.setResidualBudgetCents(residualBudget);

      return preview;
    } catch (RuntimeException e) {
      auditUtilities.logErrorPreviewTransaction(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), channel);
      throw e;
    }
  }

  public void checkPreAuth(String userId, Transaction transaction) {
    String walletStatus = walletConnector.getWallet(transaction.getInitiativeId(), userId).getStatus();
    if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
      throw new UserSuspendedException("The user has been suspended for initiative [%s]".formatted(transaction.getInitiativeId()));
    }

    if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(walletStatus)){
      throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(transaction.getInitiativeId()));
    }

    if (transaction.getTrxDate().plusMinutes(authorizationExpirationMinutes).isBefore(OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)))) {
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transaction.getId()));
    }

    if (transaction.getUserId() != null && !userId.equals(transaction.getUserId())) {
      throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED, "Transaction with transactionId [%s] is already assigned to another user".formatted(transaction.getId()));
    }

    if(SyncTrxStatus.AUTHORIZED.equals(transaction.getStatus())){
      throw new TransactionAlreadyAuthorizedException("Transaction with transactionId [%s] is already authorized".formatted(transaction.getId()));
    }

    if(!SyncTrxStatus.CREATED.equals(transaction.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(transaction.getStatus())){
      throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, "Cannot operate on transaction with transactionId [%s] in status %s".formatted(transaction.getId(), transaction.getStatus()));
    }
  }

  public void auditLogRelateUser(Transaction transaction, String channel){
    auditUtilities.logRelatedUserToTransaction(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), channel);
  }
}