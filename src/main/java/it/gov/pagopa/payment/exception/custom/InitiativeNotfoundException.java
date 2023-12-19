package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class InitiativeNotfoundException extends ServiceException {

  public InitiativeNotfoundException(String message) {
    this(ExceptionCode.INITIATIVE_NOT_FOUND, message);
  }

  public InitiativeNotfoundException(String code, String message) {
    this(code, message, null, false, null);
  }

  public InitiativeNotfoundException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
