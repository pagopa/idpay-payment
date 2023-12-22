package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class IdpaycodeNotFoundException extends ServiceException {

  public IdpaycodeNotFoundException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.IDPAYCODE_NOT_FOUND, message, null,printStackTrace, ex);
  }
  public IdpaycodeNotFoundException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
    super(code, message, payload,printStackTrace, ex);
  }
}
