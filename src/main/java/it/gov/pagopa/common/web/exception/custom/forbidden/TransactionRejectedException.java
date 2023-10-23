package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class TransactionRejectedException extends ServiceException {

  public TransactionRejectedException(String code, String message) {
    super(code, message);
  }

  public TransactionRejectedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
