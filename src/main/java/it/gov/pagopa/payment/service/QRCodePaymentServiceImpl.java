package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TrxInProgressSpecificRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final TransactionInProgress2TransactionResponseMapper
      transactionInProgress2TransactionResponseMapper;
  private final TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;
  private final RewardRuleRepository rewardRuleRepository;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TrxCodeGenUtil trxCodeGenUtil;

  public QRCodePaymentServiceImpl(
      TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
      TransactionCreationRequest2TransactionInProgressMapper
          transactionCreationRequest2TransactionInProgressMapper,
      RewardRuleRepository rewardRuleRepository,
      TransactionInProgressRepository transactionInProgressRepository,
      TrxCodeGenUtil trxCodeGenUtil) {
    this.transactionInProgress2TransactionResponseMapper =
        transactionInProgress2TransactionResponseMapper;
    this.transactionCreationRequest2TransactionInProgressMapper =
        transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.trxCodeGenUtil = trxCodeGenUtil;
  }

  @Autowired
  TransactionInProgressRepository repository;

  @Autowired
  RewardCalculatorConnector rewardCalculatorConnector;

  @Autowired
  AuthPaymentRequestMapper requestMapper;

  @Autowired
  TrxInProgressSpecificRepository specificRepository;

  @Override
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {

    if (!rewardRuleRepository.existsById(trxCreationRequest.getInitiativeId())) {

      log.error("Cannot find initiative with ID: [{}]", trxCreationRequest.getInitiativeId());

      throw new ClientExceptionWithBody(
          HttpStatus.NOT_FOUND,
          "NOT FOUND",
          "Cannot find initiative with ID: [%s]".formatted(trxCreationRequest.getInitiativeId()));
    }

    TransactionInProgress trx =
        transactionCreationRequest2TransactionInProgressMapper.apply(trxCreationRequest);

    generateTrxCodeAndSave(trx);

    return transactionInProgress2TransactionResponseMapper.apply(trx);
  }

  private void generateTrxCodeAndSave(TransactionInProgress trx) {
    long retry = 1;
    while (transactionInProgressRepository.createIfExists(trx, trxCodeGenUtil.get()).getUpsertedId() == null) {
      log.info("[CREATE_TRANSACTION] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]", retry);
    }
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
