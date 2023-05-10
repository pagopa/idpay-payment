package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.event.producer.AuthorizationNotificationProducer;
import it.gov.pagopa.payment.connector.event.producer.mapper.AuthorizationNotificationMapper;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuthPaymentMapper requestMapper;
  private final AuthorizationNotificationMapper authorizationNotificationMapper;
  private final AuthorizationNotificationProducer authorizationNotificationProducer;
  private final ErrorNotifierService errorNotifierService;

  public QRCodeAuthPaymentServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuthPaymentMapper requestMapper,
          AuthorizationNotificationMapper authorizationNotificationMapper,
          AuthorizationNotificationProducer authorizationNotificationProducer,
          ErrorNotifierService errorNotifierService) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.requestMapper = requestMapper;
    this.authorizationNotificationMapper = authorizationNotificationMapper;
    this.authorizationNotificationProducer = authorizationNotificationProducer;
    this.errorNotifierService = errorNotifierService;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    AuthPaymentDTO authPaymentDTO;
    TransactionInProgress trx =
        transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(trxCode.toLowerCase());

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

      if(SyncTrxStatus.REWARDED.equals(authPaymentDTO.getStatus())) {
        authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);
        transactionInProgressRepository.updateTrxAuthorized(trx.getId(),
                authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
        sendAuthPaymentNotification(trx, authPaymentDTO);
      } else {
        transactionInProgressRepository.updateTrxRejected(trx.getId(), authPaymentDTO.getRejectionReasons());
        sendAuthPaymentNotification(trx, authPaymentDTO);
      }

    } else if (trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
      authPaymentDTO = requestMapper.transactionMapper(trx);
    } else {
      throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "ERROR STATUS",
          String.format("The transaction's status is %s", trx.getStatus()));
    }
    return authPaymentDTO;
  }

  private void sendAuthPaymentNotification(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO) {
    try {
      log.info("[QR_CODE_AUTHORIZE_TRANSACTION][SEND_NOTIFICATION] Sending Authorization Payment event to Notification: trxId {} - userId {}", trx.getId(), trx.getUserId());
      if (!authorizationNotificationProducer.sendNotification(trx, authPaymentDTO)) {
        throw new IllegalStateException("[QR_CODE_AUTHORIZE_TRANSACTION] Something gone wrong while Auth Payment notify");
      }
    } catch (Exception e) {
      log.error("[QR_CODE_AUTHORIZE_TRANSACTION][SEND_NOTIFICATION] An error has occurred: trxId {} - userId {}", trx.getId(), trx.getUserId(), e);
      errorNotifierService.notifyAuthPayment(
              AuthorizationNotificationProducer.buildMessage(authorizationNotificationMapper.map(trx, authPaymentDTO)),
              "[QR_CODE_AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result: trxId %s - userId %s".formatted(trx.getId(), trx.getUserId()),
              true,
              e);
    }
  }
}
