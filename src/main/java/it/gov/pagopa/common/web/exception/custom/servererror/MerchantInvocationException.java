package it.gov.pagopa.common.web.exception.custom.servererror;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class MerchantInvocationException extends ServiceException {

  public MerchantInvocationException(String code, String message) {
    super(code, message);
  }

  public MerchantInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
