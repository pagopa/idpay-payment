package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePreAuthServiceImpl extends CommonPreAuthServiceImpl implements QRCodePreAuthService {
  public QRCodePreAuthServiceImpl(@Value("${app.common.expirations.authorizationMinutes}") long authorizationExpirationMinutes,
                                  TransactionInProgressRepository transactionInProgressRepository,
                                  RewardCalculatorConnector rewardCalculatorConnector,
                                  AuditUtilities auditUtilities,
                                  WalletConnector walletConnector) {
    super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector);
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    TransactionInProgress trx = transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
            .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode)));

    relateUser(trx, userId);
    AuthPaymentDTO authPaymentDTO = previewPayment(trx, RewardConstants.TRX_CHANNEL_QRCODE, SyncTrxStatus.IDENTIFIED);

    auditLogRelateUser(trx, RewardConstants.TRX_CHANNEL_QRCODE);
    return authPaymentDTO;

  }

}
