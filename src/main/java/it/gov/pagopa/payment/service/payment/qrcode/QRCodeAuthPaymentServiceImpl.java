package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Slf4j
public class QRCodeAuthPaymentServiceImpl implements QRCodeAuthPaymentService {

  private final TransactionRepository transactionRepository;
  private final QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService;
  private final CommonAuthServiceImpl commonAuthService;

  public QRCodeAuthPaymentServiceImpl(TransactionRepository transactionRepository, QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService, CommonAuthServiceImpl commonAuthService){
    this.transactionRepository = transactionRepository;
    this.qrCodeAuthorizationExpiredService = qrCodeAuthorizationExpiredService;
    this.commonAuthService = commonAuthService;
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    TransactionInProgress trx = qrCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
    Optional<Transaction> transactionOptional = transactionRepository.findByTrxCodeAndTrxEndDateGreaterThanEqual(trxCode.toLowerCase(), OffsetDateTime.now());
    if(trx == null){
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }
    if(transactionOptional.isEmpty()){
      throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }
    Transaction transaction = transactionOptional.get();
    checkUser(trxCode,userId,trx);
    return commonAuthService.authPayment(transaction,trx,userId,trxCode);
  }

  private void checkUser(String trxCode,String userId, TransactionInProgress trx){
    if (trx.getUserId()!=null && !userId.equals(trx.getUserId())) {
      throw new UserNotAllowedException(ExceptionCode.TRX_ALREADY_ASSIGNED,
              "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }
  }
}
