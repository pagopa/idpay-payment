package it.gov.pagopa.payment.exception.custom.forbidden;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class BudgetExhaustedException extends ServiceException {

  public BudgetExhaustedException(String message) {
    this(ExceptionCode.BUDGET_EXHAUSTED, message);
  }

  public BudgetExhaustedException(String code, String message) {
    this(code, message, false, null);
  }

  public BudgetExhaustedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
