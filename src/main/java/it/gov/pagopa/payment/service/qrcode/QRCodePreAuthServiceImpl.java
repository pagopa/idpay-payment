package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TransactionInProgress2TransactionResponseMapper
      transactionInProgress2TransactionResponseMapper;
  private final AuthPaymentMapper authPaymentMapper;
  private final RewardCalculatorConnector rewardCalculatorConnector;

  public QRCodePreAuthServiceImpl(
      TransactionInProgressRepository transactionInProgressRepository,
      TransactionInProgress2TransactionResponseMapper
          transactionInProgress2TransactionResponseMapper,
      AuthPaymentMapper authPaymentMapper,
      RewardCalculatorConnector rewardCalculatorConnector) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.transactionInProgress2TransactionResponseMapper =
        transactionInProgress2TransactionResponseMapper;
    this.authPaymentMapper = authPaymentMapper;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
  }

  @Override
  public TransactionResponse relateUser(String trxCode, String userId) {
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

    AuthPaymentDTO preview =
        rewardCalculatorConnector.previewTransaction(trx, authPaymentMapper.rewardMap(trx));
    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)
        && preview.getRejectionReasons().contains("NO_ACTIVE_INITIATIVES")) {
      transactionInProgressRepository.updateTrxRejected(
          trx.getId(), userId, preview.getRejectionReasons());
    }
    if (preview.getStatus().equals(SyncTrxStatus.REJECTED)
        && preview.getRejectionReasons().contains("NO_ACTIVE_INITIATIVES")) {
      throw new ClientExceptionWithBody(
          HttpStatus.FORBIDDEN, "FORBIDDEN", "The user is not onboarded to the initiative");
    }
    transactionInProgressRepository.updateTrxIdentified(trx.getId(), userId);
    return transactionInProgress2TransactionResponseMapper.apply(trx);
  }
}
