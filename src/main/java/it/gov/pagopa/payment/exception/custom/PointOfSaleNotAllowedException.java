package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class PointOfSaleNotAllowedException extends ServiceException {

  public PointOfSaleNotAllowedException(String message) {
    this(ExceptionCode.PAYMENT_POS_NOT_ALLOWED, message);
  }

  public PointOfSaleNotAllowedException(String code, String message) {
    this(code, message, false, null);
  }

  public PointOfSaleNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
