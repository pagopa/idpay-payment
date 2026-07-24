package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionMissingParametersException extends ServiceException {

  public TransactionMissingParametersException(String code, String message) {
    this(code, message, false, null);
  }

  public TransactionMissingParametersException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,printStackTrace, ex);
  }
}
