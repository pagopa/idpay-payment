package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {
  private final long authorizationExpirationMinutes;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuditUtilities auditUtilities;

  public QRCodePreAuthServiceImpl(
          @Value("${app.qrCode.expirations.authorizationMinutes:15}") long authorizationExpirationMinutes,
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector,
          AuditUtilities auditUtilities) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.auditUtilities = auditUtilities;
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    try {
      TransactionInProgress trx = transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase()).orElse(null);

      checkPreAuth(trxCode, userId, trx);

      trx.setUserId(userId);
      trx.setTrxChargeDate(OffsetDateTime.now());

      AuthPaymentDTO preview =
              rewardCalculatorConnector.previewTransaction(trx);
      if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
        transactionInProgressRepository.updateTrxRejected(
                trx.getId(), userId, preview.getRejectionReasons());
        log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
        if (preview.getRejectionReasons().contains(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)) {
          throw new ClientExceptionWithBody(
                  HttpStatus.FORBIDDEN,
                  PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED,
                  "Budget exhausted for user [%s] and initiative [%s]".formatted(userId, trx.getInitiativeId()));
        }
        throw new ClientExceptionWithBody(
                HttpStatus.FORBIDDEN,
                PaymentConstants.ExceptionCode.REJECTED,
                "Transaction with trxCode [%s] is rejected".formatted(trxCode));
      } else {
        preview.setRejectionReasons(null);
        preview.setStatus(SyncTrxStatus.IDENTIFIED);
        transactionInProgressRepository.updateTrxIdentified(trx.getId(), userId, preview.getReward(), preview.getRejectionReasons(), preview.getRewards());
      }

      auditUtilities.logRelatedUserToTransaction(trx.getInitiativeId(), trx.getId(), trxCode, userId);

      Long residualBudget = CommonUtilities.calculateResidualBudget(preview.getRewards()) != null ?
              Long.sum(CommonUtilities.calculateResidualBudget(preview.getRewards()), preview.getReward()) : null;
      preview.setResidualBudget(residualBudget);

      return preview;
    } catch (RuntimeException e) {
      auditUtilities.logErrorRelatedUserToTransaction(trxCode, userId);
      if (e.toString().contains("ClientException")){
        throw e;
      } else {
        throw new ClientExceptionWithBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                PaymentConstants.ExceptionCode.GENERIC_ERROR,
                "A generic error occurred for trxCode: [%s]".formatted(trxCode));
      }
    }
  }

  private void checkPreAuth(String trxCode, String userId, TransactionInProgress trx) {
    if (trx == null || trx.getTrxDate().plusMinutes(authorizationExpirationMinutes).isBefore(OffsetDateTime.now())) {
      throw new ClientExceptionWithBody(
              HttpStatus.NOT_FOUND,
              PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
              "Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }

    if (trx.getUserId() != null && !userId.equals(trx.getUserId())) {
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ANOTHER_USER,
              "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }

    if(SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
      throw new ClientExceptionWithBody(
              HttpStatus.FORBIDDEN,
              PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED,
              "Transaction with trxCode [%s] is already authorized".formatted(trxCode));
    }

    if(!SyncTrxStatus.CREATED.equals(trx.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())){
      throw new ClientExceptionWithBody(
              HttpStatus.BAD_REQUEST,
              PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID,
              "Cannot relate transaction [%s] in status %s".formatted(trxCode, trx.getStatus()));
    }
  }
}
