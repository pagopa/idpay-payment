package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class PinBlockInvalidException extends ServiceException {

  public PinBlockInvalidException(String message) {
    this(ExceptionCode.INVALID_PIN, message);
  }

  public PinBlockInvalidException(String code, String message) {
    this(code, message, false, null);
  }

  public PinBlockInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
