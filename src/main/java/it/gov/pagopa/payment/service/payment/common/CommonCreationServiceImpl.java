package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.badrequest.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.forbidden.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.notfound.InitiativeNotfoundException;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
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
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("CommonCreate")
public class CommonCreationServiceImpl {

  static final String CREATE_TRANSACTION = "CREATE_TRANSACTION";

  protected final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
  protected final TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper;
  protected final RewardRuleRepository rewardRuleRepository;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TrxCodeGenUtil trxCodeGenUtil;
  protected final AuditUtilities auditUtilities;
  private final MerchantConnector merchantConnector;
  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public CommonCreationServiceImpl(
          TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
          TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper,
          RewardRuleRepository rewardRuleRepository,
          TransactionInProgressRepository transactionInProgressRepository,
          TrxCodeGenUtil trxCodeGenUtil,
          AuditUtilities auditUtilities,
          MerchantConnector merchantConnector) {
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
        throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot create transaction with invalid amount: %s".formatted(trxCreationRequest.getAmountCents()));
      }

      InitiativeConfig initiative = rewardRuleRepository.findById(trxCreationRequest.getInitiativeId())
              .map(RewardRule::getInitiativeConfig)
              .orElse(null);

      checkInitiativeType(trxCreationRequest.getInitiativeId(), initiative);

      checkInitiativeValidPeriod(today, initiative);

      MerchantDetailDTO merchantDetail = merchantConnector.merchantDetail(merchantId, trxCreationRequest.getInitiativeId());

      TransactionInProgress trx =
              transactionCreationRequest2TransactionInProgressMapper.apply(
                      trxCreationRequest, channel, merchantId, acquirerId, merchantDetail, idTrxIssuer);
      generateTrxCodeAndSave(trx);

      logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), merchantId);

      return transactionInProgress2TransactionResponseMapper.apply(trx);
    } catch (RuntimeException e) {
      logErrorCreatedTransaction(trxCreationRequest.getInitiativeId(), merchantId);
      throw e;
    }
  }

  protected void checkInitiativeType(String initiativeId, InitiativeConfig initiative) {
    if (initiative == null) {
      log.info(
              "[{}] Cannot find initiative with ID: [{}]",
              getFlow(),
              initiativeId);
      throw new InitiativeNotfoundException("Cannot find initiative with id [%s]".formatted(initiativeId));
    }

    if (!InitiativeRewardType.DISCOUNT.equals(initiative.getInitiativeRewardType())) {
      log.info(
              "[{}] Initiative with ID: [{}] is not DISCOUNT type",
              getFlow(),
              initiativeId);
      throw new InitiativeNotfoundException(
              PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT,
              "The initiative with id [%s] is not discount".formatted(initiativeId));
    }
  }

  protected void checkInitiativeValidPeriod(LocalDate today, InitiativeConfig initiative) {
    if (initiative != null && (today.isBefore(initiative.getStartDate()) || today.isAfter(initiative.getEndDate()))) {
      log.info("[{}] Cannot create transaction out of valid period. Initiative startDate: [{}] endDate: [{}]",
              getFlow(),
              initiative.getStartDate(), initiative.getEndDate());
      throw new InitiativeInvalidException("Cannot create transaction out of valid period. Initiative startDate: %s endDate: %s"
                      .formatted(initiative.getStartDate(), initiative.getEndDate()));
    }
  }

  protected void generateTrxCodeAndSave(TransactionInProgress trx) {
    long retry = 1;
    while (transactionInProgressRepository.createIfExists(trx, trxCodeGenUtil.get()).getUpsertedId()
            == null) {
      log.info(
              "[{}] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
              getFlow(),
              retry);
    }
  }

  protected String getFlow() {
    return CREATE_TRANSACTION;
  }

  protected void logCreatedTransaction(String initiativeId, String id, String trxCode, String merchantId) {
    auditUtilities.logCreatedTransaction(initiativeId, id, trxCode, merchantId);
  }

  protected  void logErrorCreatedTransaction(String initiativeId, String merchantId){
    auditUtilities.logErrorCreatedTransaction(initiativeId, merchantId);
  }
}