package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuthPaymentMapper requestMapper;
  private final TransactionNotifierService notifierService;
  private final PaymentErrorNotifierService paymentErrorNotifierService;
  private final AuditUtilities auditUtilities;

  public QRCodeAuthPaymentServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuthPaymentMapper requestMapper,
          TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
          AuditUtilities auditUtilities) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.requestMapper = requestMapper;
    this.notifierService = notifierService;
    this.paymentErrorNotifierService = paymentErrorNotifierService;
    this.auditUtilities = auditUtilities;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    try {
      AuthPaymentDTO authPaymentDTO;
      TransactionInProgress trx =
              transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(trxCode.toLowerCase());

      if (trx == null) {
        throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND, "TRANSACTION NOT FOUND",
                String.format("The transaction's with trxCode %s, doesn't exist", trxCode));
      }

      if (trx.getUserId()!=null && !userId.equals(trx.getUserId())) {
        throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN, "TRX USER ASSOCIATION",
                String.format("UserId %s not associated with transaction %s", userId,
                        trx.getId()));
      }

      if (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
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
          transactionInProgressRepository.updateTrxRejected(trx.getId(), authPaymentDTO.getRejectionReasons());
          log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
        }

        trx.setStatus(authPaymentDTO.getStatus());

        sendAuthPaymentNotification(trx);

      } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
        authPaymentDTO = requestMapper.transactionMapper(trx);
      } else {
        throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "ERROR STATUS",
                String.format("The transaction's status is %s", trx.getStatus()));
      }

      auditUtilities.logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, userId, authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());

      return authPaymentDTO;
    } catch (RuntimeException e) {
      auditUtilities.logErrorAuthorizedPayment(trxCode, userId);
      throw e;
    }
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
}
