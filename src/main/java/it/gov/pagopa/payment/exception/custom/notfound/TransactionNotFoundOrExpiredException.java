package it.gov.pagopa.payment.exception.custom.notfound;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionNotFoundOrExpiredException extends ServiceException {

  public TransactionNotFoundOrExpiredException(String message) {
    this(message, false, null);
  }

  public TransactionNotFoundOrExpiredException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, message, printStackTrace, ex);
  }
}
