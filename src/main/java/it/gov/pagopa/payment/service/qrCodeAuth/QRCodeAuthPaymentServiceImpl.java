package it.gov.pagopa.payment.service.qrCodeAuth;

import feign.FeignException;
import it.gov.pagopa.payment.connector.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.TransactionInProgressConstants;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.enums.Status;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TrxInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuthPaymentRequestMapper requestMapper;
  private final TrxInProgressRepository repository;


  public QRCodeAuthPaymentServiceImpl(
      TransactionInProgressRepository transactionInProgressRepository,
      RewardCalculatorConnector rewardCalculatorConnector,
      AuthPaymentRequestMapper requestMapper, TrxInProgressRepository repository) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.requestMapper = requestMapper;
    this.repository = repository;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    TransactionInProgress transactionInProgress = transactionInProgressRepository.findAndModify(
        trxCode);
    if (transactionInProgress == null) {
      throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND, "TRANSACTION NOT FOUND",
          String.format("The transaction's with trxCode %s, doesn't exist", trxCode));
    }
    this.findByUserId(transactionInProgress.getId(), userId);
    if (!transactionInProgress.getStatus().equals(Status.IDENTIFIED)) {
      checkStatus(transactionInProgress);
    }
    AuthPaymentRequestDTO requestDTO = requestMapper.rewardMap(transactionInProgress);
    AuthPaymentResponseDTO responseDTO;
    AuthPaymentDTO authPaymentDTO = new AuthPaymentDTO();
    try {
      responseDTO = rewardCalculatorConnector.authorizePayment(
          transactionInProgress.getInitiativeId(), requestDTO);
      authPaymentDTO = requestMapper.rewardResponseMap(responseDTO, transactionInProgress);

      if (!responseDTO.getStatus().equals(TransactionInProgressConstants.AUTHORIZED)) {
        checkStatus(transactionInProgress);
      }
      transactionInProgressRepository.updateTrxAuthorized(transactionInProgress.getId(),
          responseDTO.getReward(), responseDTO.getRejectionReasons());

    } catch (FeignException e) {
      switch (e.status()) {
        case 409 -> {return authPaymentDTO;}
        case 429 -> throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
            "Too many request on the ms reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return authPaymentDTO;
  }

  private void checkStatus(TransactionInProgress transactionInProgress) {
    throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "ERROR STATUS",
        String.format("The transaction's status is %s", transactionInProgress.getStatus()));
  }

  private void findByUserId(String id, String userId) {
    repository.findByIdAndUserId(id, userId).orElseThrow(
        () -> new ClientExceptionWithBody(HttpStatus.FORBIDDEN, "TRX USER ASSOCIATION",
            String.format("UserId %s not associated with transaction %s", userId, id)));
  }

}
