package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QRCodeCreationServiceImpl implements QRCodeCreationService {

  private final TransactionInProgress2TransactionResponseMapper
      transactionInProgress2TransactionResponseMapper;
  private final TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;
  private final RewardRuleRepository rewardRuleRepository;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TrxCodeGenUtil trxCodeGenUtil;
  private final AuditUtilities auditUtilities;

  public QRCodeCreationServiceImpl(
      TransactionInProgress2TransactionResponseMapper
          transactionInProgress2TransactionResponseMapper,
      TransactionCreationRequest2TransactionInProgressMapper
          transactionCreationRequest2TransactionInProgressMapper,
      RewardRuleRepository rewardRuleRepository,
      TransactionInProgressRepository transactionInProgressRepository,
      TrxCodeGenUtil trxCodeGenUtil,
      AuditUtilities auditUtilities) {
    this.transactionInProgress2TransactionResponseMapper =
        transactionInProgress2TransactionResponseMapper;
    this.transactionCreationRequest2TransactionInProgressMapper =
        transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.trxCodeGenUtil = trxCodeGenUtil;
    this.auditUtilities = auditUtilities;
  }

  @Override
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest,
      String channel,
      String merchantId,
      String acquirerId,
      String idTrxAcquirer) {
    try {
      if (!rewardRuleRepository.existsById(trxCreationRequest.getInitiativeId())) {
        log.info(
                "[QR_CODE_CREATE_TRANSACTION] Cannot find initiative with ID: [{}]",
                trxCreationRequest.getInitiativeId());
        throw new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                "NOT FOUND",
                "Cannot find initiative with ID: [%s]".formatted(trxCreationRequest.getInitiativeId()));
      }

      TransactionInProgress trx =
              transactionCreationRequest2TransactionInProgressMapper.apply(
                      trxCreationRequest, channel, merchantId, acquirerId, idTrxAcquirer);
      generateTrxCodeAndSave(trx);

      auditUtilities.logCreatedTransaction(trx.getInitiativeId(), trx.getTrxCode(), merchantId);

      return transactionInProgress2TransactionResponseMapper.apply(trx);
    } catch (RuntimeException e) {
      auditUtilities.logErrorCreatedTransaction(trxCreationRequest.getInitiativeId(), merchantId);
      throw e;
    }
  }

  private void generateTrxCodeAndSave(TransactionInProgress trx) {
    long retry = 1;
    while (transactionInProgressRepository.createIfExists(trx, trxCodeGenUtil.get()).getUpsertedId()
        == null) {
      log.info(
          "[QR_CODE_CREATE_TRANSACTION] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
          retry);
    }
  }
}
