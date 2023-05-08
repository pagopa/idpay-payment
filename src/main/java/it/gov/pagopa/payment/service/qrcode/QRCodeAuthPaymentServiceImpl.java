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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuthPaymentMapper requestMapper;
  private final AuthorizationNotificationProducer authorizationNotificationProducer;

  public QRCodeAuthPaymentServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuthPaymentMapper requestMapper, AuthorizationNotificationProducer authorizationNotificationProducer) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.requestMapper = requestMapper;
    this.authorizationNotificationProducer = authorizationNotificationProducer;
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
    //todo mettere if (cercare su reward-calc)
    try {
      log.info("[SEND_NOTIFICATION] Sending event to Notification");
      authorizationNotificationProducer.sendNotification(trx, authPaymentDTO);
    } catch (Exception e) {
      //TODO come gestire errore? errore dovrebbe essere bloccante?
      log.error("[SEND_NOTIFICATION] An error has occurred", e);
    }
  }
}
