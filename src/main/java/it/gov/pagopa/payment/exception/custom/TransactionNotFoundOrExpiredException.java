package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionNotFoundOrExpiredException extends ServiceException {

  public TransactionNotFoundOrExpiredException(String message) {
    this(message, false, null);
  }

  public TransactionNotFoundOrExpiredException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, message, null, printStackTrace, ex);
  }

  public TransactionNotFoundOrExpiredException(String code,String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
