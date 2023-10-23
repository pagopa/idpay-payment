package it.gov.pagopa.payment.exception.custom.servererror;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.ServiceException;

public class RewardCalculatorInvocationException extends ServiceException {

  public RewardCalculatorInvocationException(String message) {
    this(message, false, null);
  }

  public RewardCalculatorInvocationException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
}
