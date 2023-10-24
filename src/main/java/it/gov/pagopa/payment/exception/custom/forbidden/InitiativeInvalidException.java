package it.gov.pagopa.payment.exception.custom.forbidden;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class InitiativeInvalidException extends ServiceException {

  public InitiativeInvalidException(String message) {
    this(ExceptionCode.INITIATIVE_INVALID_DATE, message);
  }

  public InitiativeInvalidException(String code, String message) {
    this(code, message, false, null);
  }

  public InitiativeInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
