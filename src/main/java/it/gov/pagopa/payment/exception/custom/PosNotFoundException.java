package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class PosNotFoundException extends ServiceException {

  public PosNotFoundException(String message) {
    this(message, null);
  }

  public PosNotFoundException(String message, Throwable ex) {
    super(ExceptionCode.POINT_OF_SALE_NOT_FOUND, message, false, ex);
  }
}
