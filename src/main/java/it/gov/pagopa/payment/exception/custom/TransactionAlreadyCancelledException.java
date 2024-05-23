package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class TransactionAlreadyCancelledException extends ServiceException {

  public TransactionAlreadyCancelledException(String message) {
    this(ExceptionCode.TRX_ALREADY_CANCELLED, message);
  }

  public TransactionAlreadyCancelledException(String code, String message) {
    this(code, message, false, null);
  }

  public TransactionAlreadyCancelledException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
