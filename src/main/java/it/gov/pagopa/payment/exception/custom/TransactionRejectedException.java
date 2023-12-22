package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionRejectedException extends ServiceException {

  public TransactionRejectedException(String message) {
    this(ExceptionCode.REJECTED, message);
  }

  public TransactionRejectedException(String code, String message) {
    this(code, message, null,false, null);
  }

  public TransactionRejectedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
    super(code, message, payload, printStackTrace, ex);
  }
}
