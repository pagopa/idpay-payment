package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class TransactionRejectedException extends ServiceException {

  public TransactionRejectedException(String message) {
    this(ExceptionCode.REJECTED, message);
  }

  public TransactionRejectedException(String code, String message) {
    this(code, message,false, null);
  }

  public TransactionRejectedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
