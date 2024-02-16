package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements QRCodeAuthPaymentService {

  private final QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService;

  public QRCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                      QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService,
                                      RewardCalculatorConnector rewardCalculatorConnector,
                                      AuditUtilities auditUtilities,
                                      WalletConnector walletConnector,
                                      @Qualifier("commonPreAuth")CommonPreAuthServiceImpl commonPreAuthService){
    super(transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector, commonPreAuthService);
    this.qrCodeAuthorizationExpiredService = qrCodeAuthorizationExpiredService;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    TransactionInProgress trx = qrCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
    if(trx == null){
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }
    checkUser(trxCode,userId,trx);
    return super.authPayment(trx,userId,trxCode);
  }

  private void checkUser(String trxCode,String userId, TransactionInProgress trx){
    if (trx.getUserId()!=null && !userId.equals(trx.getUserId())) {
      throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED,
              "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }
  }
}
