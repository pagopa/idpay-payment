package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
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


  public QRCodeAuthPaymentServiceImpl(
      TransactionInProgressRepository transactionInProgressRepository,
      RewardCalculatorConnector rewardCalculatorConnector,
      AuthPaymentMapper requestMapper) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.requestMapper = requestMapper;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    AuthPaymentDTO authPaymentDTO;
    AuthPaymentRequestDTO body;
    TransactionInProgress transactionInProgress =
        transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(trxCode);
    if (transactionInProgress == null) {
      throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND, "TRANSACTION NOT FOUND",
          String.format("The transaction's with trxCode %s, doesn't exist", trxCode));
    }
    if (!userId.equals(transactionInProgress.getUserId())) {
      throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN, "TRX USER ASSOCIATION",
          String.format("UserId %s not associated with transaction %s", userId,
              transactionInProgress.getId()));
    }
    if (transactionInProgress.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
      body = requestMapper.rewardMap(transactionInProgress);

      authPaymentDTO = rewardCalculatorConnector.authorizePayment(transactionInProgress, body);

      transactionInProgressRepository.updateTrxAuthorized(transactionInProgress.getId(),
          authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());

    } else if (transactionInProgress.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
      authPaymentDTO = requestMapper.transactionMapper(transactionInProgress);
    } else {
      throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "ERROR STATUS",
          String.format("The transaction's status is %s", transactionInProgress.getStatus()));
    }
    return authPaymentDTO;
  }

}
