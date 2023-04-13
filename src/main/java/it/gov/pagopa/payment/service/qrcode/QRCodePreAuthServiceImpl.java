package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final AuthPaymentMapper authPaymentMapper;
  private final RewardCalculatorConnector rewardCalculatorConnector;

  public QRCodePreAuthServiceImpl(
      TransactionInProgressRepository transactionInProgressRepository,
      AuthPaymentMapper authPaymentMapper,
      RewardCalculatorConnector rewardCalculatorConnector) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.authPaymentMapper = authPaymentMapper;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    TransactionInProgress trx =
        transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired(trxCode);

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

    trx.setUserId(userId);

    AuthPaymentDTO preview =
        rewardCalculatorConnector.previewTransaction(trx, authPaymentMapper.rewardMap(trx));
    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)) {
      transactionInProgressRepository.updateTrxRejected(
          trx.getId(), userId, preview.getRejectionReasons());
      if (preview.getRejectionReasons().contains(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)) {
        throw new ClientExceptionWithBody(
            HttpStatus.FORBIDDEN, "FORBIDDEN", "The user is not onboarded to the initiative");
      }
    } else {
      transactionInProgressRepository.updateTrxIdentified(trx.getId(), userId);
    }
    return preview;
  }
}
