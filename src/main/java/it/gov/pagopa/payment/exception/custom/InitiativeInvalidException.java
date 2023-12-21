package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class InitiativeInvalidException extends ServiceException {

  public InitiativeInvalidException(String message) {
    this(ExceptionCode.INITIATIVE_INVALID_DATE, message);
  }

  public InitiativeInvalidException(String code, String message) {
    this(code, message, null, false, null);
  }

  public InitiativeInvalidException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response,printStackTrace, ex);
  }
}
