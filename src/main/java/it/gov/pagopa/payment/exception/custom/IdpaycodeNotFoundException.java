package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class IdpaycodeNotFoundException extends ServiceException {

  public IdpaycodeNotFoundException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.IDPAYCODE_NOT_FOUND, message,printStackTrace, ex);
  }
  public IdpaycodeNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,printStackTrace, ex);
  }
}
