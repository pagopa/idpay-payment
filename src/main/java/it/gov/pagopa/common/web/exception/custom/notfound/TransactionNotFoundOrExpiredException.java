package it.gov.pagopa.common.web.exception.custom.notfound;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class TransactionNotFoundOrExpiredException extends ServiceException {

  public TransactionNotFoundOrExpiredException(String code, String message) {
    super(code, message);
  }

  public TransactionNotFoundOrExpiredException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
