package it.gov.pagopa.payment.utils;

public final class RewardConstants {
    private RewardConstants(){}

    //region transactions' channels
    public static final String TRX_CHANNEL_QRCODE = "QRCODE";
    public static final String TRX_CHANNEL_IDPAYCODE = "IDPAYCODE";
    //endregion

    //region reward evaluation rejection reasons
    public static final String TRX_REJECTION_REASON_NO_INITIATIVE = "NO_ACTIVE_INITIATIVES";
    public static final String INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED = "BUDGET_EXHAUSTED";
    //endregion
}
