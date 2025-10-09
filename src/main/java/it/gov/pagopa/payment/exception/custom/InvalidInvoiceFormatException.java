package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class InvalidInvoiceFormatException extends ServiceException {

  public InvalidInvoiceFormatException(String code, String message) {
    this(code, message, false, null);
  }

  public InvalidInvoiceFormatException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
