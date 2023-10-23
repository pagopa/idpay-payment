package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class BudgetExhaustedException extends ServiceException {

  public BudgetExhaustedException(String code, String message) {
    super(code, message);
  }

  public BudgetExhaustedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
