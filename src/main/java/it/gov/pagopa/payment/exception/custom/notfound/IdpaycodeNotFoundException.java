package it.gov.pagopa.payment.exception.custom.notfound;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class IdpaycodeNotFoundException extends ServiceException {

  public IdpaycodeNotFoundException(String message) {
    this(ExceptionCode.IDPAYCODE_NOT_FOUND, message);
  }

  public IdpaycodeNotFoundException(String code, String message) {
    this(code, message, false, null);
  }

  public IdpaycodeNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
