package it.gov.pagopa.payment.exception.custom.notfound;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class MerchantNotFoundException extends ServiceException {

  public MerchantNotFoundException(String message) {
    this(ExceptionCode.MERCHANT_NOT_FOUND, message);
  }

  public MerchantNotFoundException(String code, String message) {
    this(code, message, false, null);
  }

  public MerchantNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
