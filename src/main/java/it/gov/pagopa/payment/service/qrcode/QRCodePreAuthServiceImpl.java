package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.http.HttpStatus;

public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {

  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
  private final AuthPaymentRequestMapper authPaymentRequestMapper;
  private final RewardCalculatorConnector rewardCalculatorConnector;

  public QRCodePreAuthServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
      TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
      AuthPaymentRequestMapper authPaymentRequestMapper, RewardCalculatorConnector rewardCalculatorConnector) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    this.authPaymentRequestMapper = authPaymentRequestMapper;
    this.rewardCalculatorConnector = rewardCalculatorConnector;
  }

  @Override
  public TransactionResponse relateUser(String userId, String trxCode) {
    TransactionInProgress trx = transactionInProgressRepository.findByTrxCodeAndRelateUser(trxCode, userId);
    AuthPaymentResponseDTO preview = rewardCalculatorConnector.previewTransaction(trx.getInitiativeId(), authPaymentRequestMapper.rewardMap(trx));
    if(preview.getStatus().equals(SyncTrxStatus.REJECTED.name())){
      transactionInProgressRepository.updateTrxRejected(trx.getId(), preview.getRejectionReasons());
      throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "The user is not onboarded to the initiative");
    }
    transactionInProgressRepository.updateTrxIdentified(trx.getId());
    return transactionInProgress2TransactionResponseMapper.apply(trx);
  }
}
