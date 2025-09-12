package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class ProductNotValidException extends ServiceException {

  public ProductNotValidException(String message) {
    this(ExceptionCode.PRODUCT_NOT_VALID, message);
  }

  public ProductNotValidException(String code, String message) {
    this(code, message, false, null);
  }

  public ProductNotValidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
