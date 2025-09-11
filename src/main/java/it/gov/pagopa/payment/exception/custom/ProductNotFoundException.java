package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class ProductNotFoundException extends ServiceException {

  public ProductNotFoundException(String message) {
    this(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, message);
  }

  public ProductNotFoundException(String code, String message) {
    this(code, message, false, null);
  }

  public ProductNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
