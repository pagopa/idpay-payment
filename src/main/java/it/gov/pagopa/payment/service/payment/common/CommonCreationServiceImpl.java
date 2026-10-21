package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service("commonCreate")
public class CommonCreationServiceImpl {

  static final String CREATE_TRANSACTION = "CREATE_TRANSACTION";

  protected final RewardRuleRepository rewardRuleRepository;
  protected final AuditUtilities auditUtilities;
  private final MerchantConnector merchantConnector;
  private final TransactionService transactionService;
  private final TransactionMapper transactionMapper;

  public CommonCreationServiceImpl(
          RewardRuleRepository rewardRuleRepository,
          AuditUtilities auditUtilities,
          MerchantConnector merchantConnector,
          TransactionService transactionService,
          TransactionMapper transactionMapper) {
    this.rewardRuleRepository = rewardRuleRepository;
    this.auditUtilities = auditUtilities;
    this.merchantConnector = merchantConnector;
    this.transactionService = transactionService;
    this.transactionMapper = transactionMapper;
  }

  public TransactionResponse createTransaction(
          TransactionCreationRequest trxCreationRequest,
          String channel,
          String merchantId,
          String acquirerId,
          String idTrxIssuer) {

    LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
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

      Transaction transaction = transactionMapper.
              transactionCreationRequestToTransaction(
                      trxCreationRequest, channel, merchantId, acquirerId, merchantDetail, idTrxIssuer);
      transactionService.generateTrxCodeAndSave(transaction, getFlow());

      logCreatedTransaction(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), merchantId);

      return transactionMapper.transactionToTransactionResponse(transaction);
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
              Utilities.sanitizeString(initiativeId));
      throw new InitiativeNotfoundException("Cannot find initiative with id [%s]".formatted(initiativeId));
    }

    if (!InitiativeRewardType.DISCOUNT.equals(initiative.getInitiativeRewardType())) {
      log.info(
              "[{}] Initiative with ID: [{}] is not DISCOUNT type",
              flowName,
              Utilities.sanitizeString(initiativeId));
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