package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class BudgetExhaustedException extends ServiceException {

  public BudgetExhaustedException(String message) {
    this(ExceptionCode.BUDGET_EXHAUSTED, message);
  }

  public BudgetExhaustedException(String code, String message) {
    this(code, message,null, false, null);
  }

  public BudgetExhaustedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
