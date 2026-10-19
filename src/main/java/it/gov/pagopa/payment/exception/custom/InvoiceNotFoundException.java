package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class InvoiceNotFoundException extends ServiceException {

  public InvoiceNotFoundException(String message) {
    this(message, null);
  }

  public InvoiceNotFoundException(String message, Throwable ex) {
    super(ExceptionCode.INVOICE_NOT_FOUND, message, false, ex);
  }
}
