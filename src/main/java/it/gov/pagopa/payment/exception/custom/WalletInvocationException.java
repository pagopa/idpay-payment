package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class WalletInvocationException extends ServiceException {

  public WalletInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.GENERIC_ERROR, message, null, printStackTrace, ex);
  }
  public WalletInvocationException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response,printStackTrace, ex);
  }
}
