package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePreAuthServiceImpl extends CommonPreAuthServiceImpl implements QRCodePreAuthService {
  private final TransactionInProgressRepository transactionInProgressRepository;
  public QRCodePreAuthServiceImpl(@Value("${app.qrCode.expirations.authorizationMinutes:15}") long authorizationExpirationMinutes,
                                  TransactionInProgressRepository transactionInProgressRepository,
                                  RewardCalculatorConnector rewardCalculatorConnector,
                                  AuditUtilities auditUtilities,
                                  WalletConnector walletConnector) {
    super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector);
    this.transactionInProgressRepository = transactionInProgressRepository;
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    TransactionInProgress trx = transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
            .orElseThrow(() -> new ClientExceptionWithBody(
                    HttpStatus.NOT_FOUND,
                    PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                    "Cannot find transaction with trxCode [%s]".formatted(trxCode)));

    relateUser(trx, userId);
    AuthPaymentDTO authPaymentDTO = previewPayment(trx, RewardConstants.TRX_CHANNEL_QRCODE);

    auditLogRelateUser(trx, RewardConstants.TRX_CHANNEL_QRCODE);
    return authPaymentDTO;

  }

}
