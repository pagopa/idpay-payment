package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.qrcode.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final QRCodeAuthorizationExpiredService authorizationExpiredService;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final TransactionNotifierService notifierService;
  private final PaymentErrorNotifierService paymentErrorNotifierService;
  private final AuditUtilities auditUtilities;
  private final WalletConnector walletConnector;

  public QRCodeAuthPaymentServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          QRCodeAuthorizationExpiredService authorizationExpiredService,
          RewardCalculatorConnector rewardCalculatorConnector,
          TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
          AuditUtilities auditUtilities,
          WalletConnector walletConnector) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.authorizationExpiredService = authorizationExpiredService;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.notifierService = notifierService;
    this.paymentErrorNotifierService = paymentErrorNotifierService;
    this.auditUtilities = auditUtilities;
    this.walletConnector = walletConnector;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    try {
      TransactionInProgress trx = authorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());

      if (trx == null) {
        throw new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                "Cannot find transaction with trxCode [%s]".formatted(trxCode));
      }

      String walletStatus = walletConnector.getWallet(trx.getInitiativeId(), userId).getStatus();

      checkAuth(trxCode, userId, trx, walletStatus);
      AuthPaymentDTO authPaymentDTO = invokeRuleEngine(userId, trxCode, trx);

      auditUtilities.logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, userId, authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
      authPaymentDTO.setResidualBudget(CommonUtilities.calculateResidualBudget(trx.getRewards()));
      authPaymentDTO.setRejectionReasons(null);
      return authPaymentDTO;
    } catch (RuntimeException e) {
      auditUtilities.logErrorAuthorizedPayment(trxCode, userId);
      throw e;
    }
  }

  private AuthPaymentDTO invokeRuleEngine(String userId, String trxCode, TransactionInProgress trx){
    AuthPaymentDTO authPaymentDTO;
    if (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
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
        transactionInProgressRepository.updateTrxRejected(trx.getId(), authPaymentDTO.getRejectionReasons(), trx.getTrxChargeDate());
        log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
        if (authPaymentDTO.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
          throw new ClientExceptionWithBody(
                  HttpStatus.FORBIDDEN,
                  PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED,
                  "Budget exhausted for user [%s] and initiative [%s]".formatted(userId, trx.getInitiativeId()));
        }
        throw new ClientExceptionWithBody(
                HttpStatus.FORBIDDEN,
                PaymentConstants.ExceptionCode.REJECTED,
                "Transaction with trxCode [%s] is rejected".formatted(trxCode));
      }

      trx.setStatus(authPaymentDTO.getStatus());

      sendAuthPaymentNotification(trx);

    } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED,
              "Transaction with trxCode [%s] is already authorized".formatted(trxCode));
    } else {
      throw new ClientExceptionWithBody(
              HttpStatus.BAD_REQUEST,
              PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID,
              "Cannot relate transaction in status " + trx.getStatus());
    }
    return authPaymentDTO;
  }

  private void sendAuthPaymentNotification(TransactionInProgress trx) {
    try {
      log.info("[AUTHORIZE_TRANSACTION][SEND_NOTIFICATION] Sending Authorization Payment event to Notification: trxId {} - userId {}", trx.getId(), trx.getUserId());
      if (!notifierService.notify(trx, trx.getUserId())) {
        throw new IllegalStateException("[AUTHORIZE_TRANSACTION] Something gone wrong while Auth Payment notify");
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

  private void checkAuth(String trxCode, String userId, TransactionInProgress trx, String walletStatus){
    if (PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR,
              "User %s has been suspended for initiative %s".formatted(userId, trx.getInitiativeId()));
    }

    if (trx.getUserId()!=null && !userId.equals(trx.getUserId())) {
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ANOTHER_USER,
              "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }
  }

}
