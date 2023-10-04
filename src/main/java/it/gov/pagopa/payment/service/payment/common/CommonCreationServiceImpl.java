package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service("CommonCreate")
public class CommonCreationServiceImpl {

  static final String CREATE_TRANSACTION = "CREATE_TRANSACTION";

  private final TransactionInProgress2TransactionResponseMapper
      transactionInProgress2TransactionResponseMapper;
  private final TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;
  private final RewardRuleRepository rewardRuleRepository;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TrxCodeGenUtil trxCodeGenUtil;
  private final AuditUtilities auditUtilities;
  private final MerchantConnector merchantConnector;
  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public CommonCreationServiceImpl(
          TransactionInProgress2TransactionResponseMapper
          transactionInProgress2TransactionResponseMapper,
          TransactionCreationRequest2TransactionInProgressMapper
          transactionCreationRequest2TransactionInProgressMapper,
          RewardRuleRepository rewardRuleRepository,
          TransactionInProgressRepository transactionInProgressRepository,
          TrxCodeGenUtil trxCodeGenUtil,
          AuditUtilities auditUtilities, MerchantConnector merchantConnector) {
    this.transactionInProgress2TransactionResponseMapper =
        transactionInProgress2TransactionResponseMapper;
    this.transactionCreationRequest2TransactionInProgressMapper =
        transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.trxCodeGenUtil = trxCodeGenUtil;
    this.auditUtilities = auditUtilities;
    this.merchantConnector = merchantConnector;
  }

  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest,
      String channel,
      String merchantId,
      String acquirerId,
      String idTrxIssuer) {

    LocalDate today = LocalDate.now();
    try {
      if (trxCreationRequest.getAmountCents() <= 0L) {
        log.info("[{}] Cannot create transaction with invalid amount: [{}]", getFlow(), trxCreationRequest.getAmountCents());
        throw new ClientExceptionWithBody(
                HttpStatus.BAD_REQUEST,
                "INVALID AMOUNT",
                "Cannot create transaction with invalid amount: %s".formatted(trxCreationRequest.getAmountCents()));
      }

      InitiativeConfig initiative = rewardRuleRepository.findById(trxCreationRequest.getInitiativeId())
              .map(RewardRule::getInitiativeConfig)
              .orElse(null);
      if (initiative == null || !InitiativeRewardType.DISCOUNT.equals(initiative.getInitiativeRewardType())) {
        log.info(
                "[{}] Cannot find initiative with ID: [{}]",
                getFlow(),
                trxCreationRequest.getInitiativeId());
        throw new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                "NOT FOUND",
                "Cannot find initiative with ID: [%s]".formatted(trxCreationRequest.getInitiativeId()));
      }

      if (today.isBefore(initiative.getStartDate()) || today.isAfter(initiative.getEndDate())) {
        log.info("[{}] Cannot create transaction out of valid period. Initiative startDate: [{}] endDate: [{}]",
                getFlow(),
                initiative.getStartDate(), initiative.getEndDate());
        throw new ClientExceptionWithBody(
                HttpStatus.BAD_REQUEST,
                "INVALID DATE",
                "Cannot create transaction out of valid period. Initiative startDate: %s endDate: %s"
                        .formatted(initiative.getStartDate(), initiative.getEndDate()));
      }

      MerchantDetailDTO merchantDetail = merchantConnector.merchantDetail(merchantId, trxCreationRequest.getInitiativeId());

      TransactionInProgress trx =
              transactionCreationRequest2TransactionInProgressMapper.apply(
                      trxCreationRequest, channel, merchantId, acquirerId, merchantDetail, idTrxIssuer);
      generateTrxCodeAndSave(trx);

      auditUtilities.logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), merchantId);

      return transactionInProgress2TransactionResponseMapper.apply(trx);
    } catch (RuntimeException e) {
      auditUtilities.logErrorCreatedTransaction(trxCreationRequest.getInitiativeId(), merchantId);
      throw e;
    }
  }

  protected String getFlow() {
    return CREATE_TRANSACTION;
  }


  private void generateTrxCodeAndSave(TransactionInProgress trx) {
    long retry = 1;
    while (transactionInProgressRepository.createIfExists(trx, trxCodeGenUtil.get()).getUpsertedId()
        == null) {
      log.info(
          "[{}] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
          getFlow(),
          retry);
    }
  }
}