package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.TransactionSynchronousException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final RewardCalculatorConnector rewardCalculatorConnector;
  private final AuditUtilities auditUtilities;

  public QRCodePreAuthServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          RewardCalculatorConnector rewardCalculatorConnector, AuditUtilities auditUtilities) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
    this.auditUtilities = auditUtilities;
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    try {
      TransactionInProgress trx =
              transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());

      if (trx == null) {
        throw new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                "NOT FOUND",
                "Cannot find transaction with trxCode [%s]".formatted(trxCode));
      }

      if (trx.getUserId() != null && !userId.equals(trx.getUserId())) {
        throw new ClientExceptionWithBody(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
      }

      if(!SyncTrxStatus.CREATED.equals(trx.getStatus()) && !SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())){
        throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "Cannot relate transaction in status " + trx.getStatus());
      }

      trx.setUserId(userId);

      AuthPaymentDTO preview =
              rewardCalculatorConnector.previewTransaction(trx);
      if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
        transactionInProgressRepository.updateTrxRejected(
                trx.getId(), userId, preview.getRejectionReasons());
        log.info("[TRX_STATUS][REJECTED] The transaction with trxId {} trxCode {}, has been rejected ",trx.getId(), trx.getTrxCode());
        if (preview.getRejectionReasons().contains(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)) {
          throw new TransactionSynchronousException(HttpStatus.FORBIDDEN, preview);
        }
      } else {
        preview.setStatus(SyncTrxStatus.IDENTIFIED);
        transactionInProgressRepository.updateTrxIdentified(trx.getId(), userId, preview.getReward(), preview.getRejectionReasons(), preview.getRewards());
      }

      auditUtilities.logRelatedUserToTransaction(trx.getInitiativeId(), trx.getId(), trxCode, userId);

      BigDecimal residualBudget = CommonUtilities.calculateResidualBudget(preview.getRewards()) != null ?
              CommonUtilities.calculateResidualBudget(preview.getRewards()).add(CommonUtilities.centsToEuro(preview.getReward())) : null;
      preview.setResidualBudget(residualBudget);

      return preview;
    } catch (RuntimeException e) {
      auditUtilities.logErrorRelatedUserToTransaction(trxCode, userId);
      throw e;
    }
  }
}
