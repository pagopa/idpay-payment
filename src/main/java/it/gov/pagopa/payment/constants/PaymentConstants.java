package it.gov.pagopa.payment.constants;

public class PaymentConstants {

  public static final String OPERATION_TYPE_CHARGE = "00";
  public static final String OPERATION_TYPE_REFUND = "01";
  public static final String CURRENCY_EUR = "EUR";
  public static final String AUTH_PAYMENT = "AUTH_PAYMENT";

  public static final class ExceptionCode {
    public static final String TRX_NOT_FOUND_OR_EXPIRED = "PAYMENT_NOT_FOUND_EXPIRED";
    public static final String TRX_ANOTHER_USER = "PAYMENT_USER_NOT_VALID";
    public static final String TRX_STATUS_NOT_VALID = "PAYMENT_STATUS_NOT_VALID";
    public static final String BUDGET_EXHAUSTED = "PAYMENT_BUDGET_EXHAUSTED";
    public static final String REJECTED = "PAYMENT_GENERIC_REJECTED";
    public static final String TOO_MANY_REQUESTS = "PAYMENT_TOO_MANY_REQUESTS";
    public static final String GENERIC_ERROR = "PAYMENT_GENERIC_ERROR";
    private ExceptionCode() {}
  }

  private PaymentConstants() {}
}
