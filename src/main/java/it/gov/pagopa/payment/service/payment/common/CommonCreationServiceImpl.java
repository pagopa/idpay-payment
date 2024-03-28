package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionInProgressService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service("commonCreate")
public class CommonCreationServiceImpl {

  static final String CREATE_TRANSACTION = "CREATE_TRANSACTION";

  protected final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
  protected final TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper;
  protected final RewardRuleRepository rewardRuleRepository;
  protected final AuditUtilities auditUtilities;
  private final MerchantConnector merchantConnector;
  private final TransactionInProgressService transactionInProgressService;

  public CommonCreationServiceImpl(
          TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
          TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper,
          RewardRuleRepository rewardRuleRepository,
          AuditUtilities auditUtilities,
          MerchantConnector merchantConnector,
          TransactionInProgressService transactionInProgressService) {
    this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    this.transactionCreationRequest2TransactionInProgressMapper = transactionCreationRequest2TransactionInProgressMapper;
    this.rewardRuleRepository = rewardRuleRepository;
    this.auditUtilities = auditUtilities;
    this.merchantConnector = merchantConnector;
    this.transactionInProgressService = transactionInProgressService;
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

      checkInitiativeType(trxCreationRequest.getInitiativeId(), initiative, getFlow());

      checkInitiativeValidPeriod(today, initiative, getFlow());

      MerchantDetailDTO merchantDetail = merchantConnector.merchantDetail(merchantId, trxCreationRequest.getInitiativeId());

      TransactionInProgress trx =
              transactionCreationRequest2TransactionInProgressMapper.apply(
                      trxCreationRequest, channel, merchantId, acquirerId, merchantDetail, idTrxIssuer);
      transactionInProgressService.generateTrxCodeAndSave(trx, getFlow());

      logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), merchantId);

      return transactionInProgress2TransactionResponseMapper.apply(trx);
    } catch (RuntimeException e) {
      logErrorCreatedTransaction(trxCreationRequest.getInitiativeId(), merchantId);
      throw e;
    }
  }

  public static void checkInitiativeType(String initiativeId, InitiativeConfig initiative, String flowName) {
    if (initiative == null) {
      log.info(
              "[{}] Cannot find initiative with ID: [{}]",
              flowName,
              initiativeId);
      throw new InitiativeNotfoundException("Cannot find initiative with id [%s]".formatted(initiativeId));
    }

    if (!InitiativeRewardType.DISCOUNT.equals(initiative.getInitiativeRewardType())) {
      log.info(
              "[{}] Initiative with ID: [{}] is not DISCOUNT type",
              flowName,
              initiativeId);
      throw new InitiativeNotfoundException(
              PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT,
              "The initiative with id [%s] is not discount".formatted(initiativeId));
    }
  }

  public static void checkInitiativeValidPeriod(LocalDate today, InitiativeConfig initiative, String flowName) {
    if (initiative != null && (today.isBefore(initiative.getStartDate()) || today.isAfter(initiative.getEndDate()))) {
      log.info("[{}] Cannot create transaction out of valid period. Initiative startDate: [{}] endDate: [{}]",
              flowName,
              initiative.getStartDate(), initiative.getEndDate());
      throw new InitiativeInvalidException("Cannot create transaction out of valid period. Initiative startDate: %s endDate: %s"
                      .formatted(initiative.getStartDate(), initiative.getEndDate()));
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