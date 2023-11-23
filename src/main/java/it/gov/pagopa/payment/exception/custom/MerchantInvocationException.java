package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class MerchantInvocationException extends ServiceException {

  public MerchantInvocationException(String message) {
    this(message, false, null);
  }

  public MerchantInvocationException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
}
