package it.gov.pagopa.common.web.exception.custom.notfound;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class MerchantNotFoundException extends ServiceException {

  public MerchantNotFoundException(String code, String message) {
    super(code, message);
  }

  public MerchantNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
