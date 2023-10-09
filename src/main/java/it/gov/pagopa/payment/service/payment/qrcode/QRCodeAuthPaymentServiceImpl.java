package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements QRCodeAuthPaymentService {
private final QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService;
  public QRCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                      RewardCalculatorConnector rewardCalculatorConnector,
                                      TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
                                      AuditUtilities auditUtilities,
                                      WalletConnector walletConnector,
                                      QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService){
    super(transactionInProgressRepository, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities, walletConnector);
    this.qrCodeAuthorizationExpiredService = qrCodeAuthorizationExpiredService;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    TransactionInProgress trx = qrCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
    return super.authPayment(trx,userId,trxCode);
  }
}
