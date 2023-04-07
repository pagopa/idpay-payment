package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionCreatedMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final TransactionInProgress2TransactionCreatedMapper
      transactionInProgress2TransactionCreatedMapper;
  private final TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;
  private final RewardRuleRepository rewardRuleRepository;
  private final TransactionInProgressRepository transactionInProgressRepository;

  public QRCodePaymentServiceImpl(
      TransactionInProgress2TransactionCreatedMapper transactionInProgress2TransactionCreatedMapper,
      TransactionCreationRequest2TransactionInProgressMapper
          transactionCreationRequest2TransactionInProgressMapper,
      RewardRuleRepository rewardRuleRepository,
      TransactionInProgressRepository transactionInProgressRepository) {
    this.transactionInProgress2TransactionCreatedMapper =
        transactionInProgress2TransactionCreatedMapper;
    this.transactionCreationRequest2TransactionInProgressMapper =
        transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.transactionInProgressRepository = transactionInProgressRepository;
  }

  @Override
  public TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest) {

    if (!rewardRuleRepository.checkIfExists(trxCreationRequest.getInitiativeId())) {
      throw new ClientExceptionWithBody(
          HttpStatus.NOT_FOUND,
          "NOT FOUND",
          "Cannot find initiative with ID: [%s]".formatted(trxCreationRequest.getInitiativeId()));
    }

    TransactionInProgress trx =
        transactionInProgressRepository.createIfNotExists(
            transactionCreationRequest2TransactionInProgressMapper.apply(trxCreationRequest));

    return transactionInProgress2TransactionCreatedMapper.apply(trx);
  }
}
