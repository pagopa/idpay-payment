package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class MerchantInvocationException extends ServiceException {

  public MerchantInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
  public MerchantInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
