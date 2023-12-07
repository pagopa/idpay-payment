package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class MerchantOrAcquirerNotAllowedException extends ServiceException {

  public MerchantOrAcquirerNotAllowedException(String message) {
    this(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, message);
  }

  public MerchantOrAcquirerNotAllowedException(String code, String message) {
    this(code, message, false, null);
  }

  public MerchantOrAcquirerNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
