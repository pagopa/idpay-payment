package it.gov.pagopa.payment.service;

import feign.FeignException;
import it.gov.pagopa.payment.connector.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.TransactionInProgressConstants;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.exception.TransactionSynchronousException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TrxInProgressSpecificRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePaymentServiceImpl implements
    QRCodePaymentService {

  @Autowired
  TransactionInProgressRepository repository;

  @Autowired
  RewardCalculatorConnector rewardCalculatorConnector;

  @Autowired
  AuthPaymentRequestMapper requestMapper;

  @Autowired
  TrxInProgressSpecificRepository specificRepository;

  @Override
  public TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest) {

    // Controllo esistenza iniziativa su reward_rule
    // Non esiste -> 404

    // TODO Genero trxCode

    // Ritorno nuova transaction_in_progress in stato CREATED

    return null;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    long startTime = System.currentTimeMillis();
    TransactionInProgress transactionInProgress = specificRepository.findAndModify(userId, trxCode);
    if (transactionInProgress == null) {
      throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND, "TRANSACTION NOT FOUND",
          String.format("The transaction's with trxCode %s and userId %s, doesn't exist", trxCode, userId));
    }
    checkStatusIdentified(transactionInProgress);
    AuthPaymentRequestDTO requestDTO = requestMapper.rewardMap(transactionInProgress);
    AuthPaymentResponseDTO responseDTO;
    AuthPaymentDTO authPaymentDTO = new AuthPaymentDTO();
    try {
      long start = System.currentTimeMillis();
      responseDTO = rewardCalculatorConnector.authorizePayment(
          transactionInProgress.getInitiativeId(), requestDTO);
      performanceLog(start, "AUTH_PAYMENT_REWARD");
      authPaymentDTO = requestMapper.rewardResponseMap(responseDTO, transactionInProgress);

    } catch (FeignException e) {
      switch (e.status()) {
        case 404, 403 -> throw new TransactionSynchronousException(authPaymentDTO);
        case 409 -> {
          return authPaymentDTO;
        }
        case 429 -> throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "Reward",
            "Too many request on reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    performanceLog(startTime, "AUTH_PAYMENT");
    return authPaymentDTO;
  }

  private void checkStatusIdentified(TransactionInProgress transactionInProgress) {
    if (!transactionInProgress.getStatus().equals(TransactionInProgressConstants.IDENTIFIED)) {
      throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "STATUS",
          String.format("The transaction's status is %s", transactionInProgress.getStatus()));
    }
  }

  private TransactionInProgress findByUserIdAndTrxCode(String userId, String trxCode) {
    return repository.findByUserIdAndTrxCode(
            userId, trxCode)
        .orElseThrow(() -> new ClientExceptionWithBody(HttpStatus.NOT_FOUND, "", ""));
  }

  private void performanceLog(long startTime, String service) {
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }

}
