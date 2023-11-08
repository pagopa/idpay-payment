package it.gov.pagopa.payment.constants;

public class PaymentConstants {

  public static final String OPERATION_TYPE_CHARGE = "00";
  public static final String OPERATION_TYPE_REFUND = "01";
  public static final String CURRENCY_EUR = "EUR";
  public static final String AUTH_PAYMENT = "AUTH_PAYMENT";

  public static final String WALLET_STATUS_SUSPENDED = "SUSPENDED";
  public static final String WALLET_STATUS_UNSUBSCRIBED = "UNSUBSCRIBED";


  public static final class ExceptionCode {
    public static final String TRX_NOT_FOUND_OR_EXPIRED = "PAYMENT_NOT_FOUND_OR_EXPIRED";
    public static final String TRX_USER_NOT_ASSOCIATED = "PAYMENT_USER_NOT_ASSOCIATED";
    public static final String TRX_ALREADY_ASSIGNED = "PAYMENT_ALREADY_ASSIGNED";
    public static final String USER_UNSUBSCRIBED = "PAYMENT_USER_UNSUBSCRIBED";
    public static final String TRX_ALREADY_AUTHORIZED = "PAYMENT_ALREADY_AUTHORIZED";
    public static final String TRX_STATUS_NOT_VALID = "PAYMENT_STATUS_NOT_VALID";
    public static final String TRX_OPERATION_NOT_ALLOWED = "PAYMENT_NOT_ALLOWED_FOR_TRX_STATUS";
    public static final String TRX_DELETE_NOT_ALLOWED = "PAYMENT_DELETE_NOT_ALLOWED_FOR_TRX_STATUS";
    public static final String TRX_UNRELATE_NOT_ALLOWED = "PAYMENT_UNRELATE_NOT_ALLOWED_FOR_TRX_STATUS";

    public static final String BUDGET_EXHAUSTED = "PAYMENT_BUDGET_EXHAUSTED";
    public static final String REJECTED = "PAYMENT_GENERIC_REJECTED";
    public static final String TOO_MANY_REQUESTS = "PAYMENT_TOO_MANY_REQUESTS";
    public static final String GENERIC_ERROR = "PAYMENT_GENERIC_ERROR";
    public static final String USER_SUSPENDED_ERROR = "PAYMENT_USER_SUSPENDED";
    public static final String INVALID_PIN = "PAYMENT_INVALID_PINBLOCK";
    public static final String USER_NOT_ONBOARDED = "PAYMENT_USER_NOT_ONBOARDED";
    public static final String AMOUNT_NOT_VALID = "PAYMENT_AMOUNT_NOT_VALID";
    public static final String INITIATIVE_NOT_FOUND = "PAYMENT_INITIATIVE_NOT_FOUND";
    public static final String INITIATIVE_NOT_DISCOUNT = "PAYMENT_INITIATIVE_NOT_DISCOUNT";
    public static final String INITIATIVE_INVALID_DATE = "PAYMENT_INITIATIVE_INVALID_DATE";
    public static final String PAYMENT_MERCHANT_NOT_ALLOWED = "PAYMENT_NOT_ALLOWED_MISMATCHED_MERCHANT";
    public static final String PAYMENT_TRANSACTION_EXPIRED = "PAYMENT_TRANSACTION_EXPIRED";
    public static final String IDPAYCODE_NOT_FOUND = "PAYMENT_IDPAYCODE_NOT_FOUND";
    public static final String MERCHANT_NOT_ONBOARDED = "PAYMENT_MERCHANT_NOT_ONBOARDED";
    public static final String PAYMENT_INVALID_REQUEST = "PAYMENT_INVALID_REQUEST";

    private ExceptionCode() {}
  }

  private PaymentConstants() {}
}
