package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionAlreadyAuthorizedException extends ServiceException {

  public TransactionAlreadyAuthorizedException(String message) {
    this(ExceptionCode.TRX_ALREADY_AUTHORIZED, message);
  }

  public TransactionAlreadyAuthorizedException(String code, String message) {
    this(code, message, false, null);
  }

  public TransactionAlreadyAuthorizedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
