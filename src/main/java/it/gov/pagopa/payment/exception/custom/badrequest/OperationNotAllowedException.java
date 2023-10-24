package it.gov.pagopa.payment.exception.custom.badrequest;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class OperationNotAllowedException extends ServiceException {
  public OperationNotAllowedException(String message) {
    this(ExceptionCode.TRX_STATUS_NOT_VALID, message);
  }

  public OperationNotAllowedException(String code, String message) {
    this(code, message, false, null);
  }

  public OperationNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
