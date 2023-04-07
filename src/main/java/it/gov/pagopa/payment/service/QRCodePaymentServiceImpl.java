package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionCreatedMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
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
  private final TrxCodeGenUtil trxCodeGenUtil;

  public QRCodePaymentServiceImpl(
      TransactionInProgress2TransactionCreatedMapper transactionInProgress2TransactionCreatedMapper,
      TransactionCreationRequest2TransactionInProgressMapper
          transactionCreationRequest2TransactionInProgressMapper,
      RewardRuleRepository rewardRuleRepository,
      TransactionInProgressRepository transactionInProgressRepository,
      TrxCodeGenUtil trxCodeGenUtil) {
    this.transactionInProgress2TransactionCreatedMapper =
        transactionInProgress2TransactionCreatedMapper;
    this.transactionCreationRequest2TransactionInProgressMapper =
        transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.trxCodeGenUtil = trxCodeGenUtil;
  }

  @Override
  public TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest) {

    if (!rewardRuleRepository.checkIfExists(trxCreationRequest.getInitiativeId())) {
      throw new ClientExceptionWithBody(
          HttpStatus.NOT_FOUND,
          "NOT FOUND",
          "Cannot find initiative with ID: [%s]".formatted(trxCreationRequest.getInitiativeId()));
    }

    TransactionInProgress trx = transactionCreationRequest2TransactionInProgressMapper.apply(trxCreationRequest);

    generateTrxCodeAndSave(trx);

    return transactionInProgress2TransactionCreatedMapper.apply(trx);
  }

  private void generateTrxCodeAndSave(TransactionInProgress trx) {
    boolean exists = true;
    String trxCode = null;
    while (exists) {
      trxCode = trxCodeGenUtil.get();
      exists = transactionInProgressRepository.existsByTrxCode(trxCode);
    }
    trx.setTrxCode(trxCode);
    transactionInProgressRepository.save(trx);
  }
}
