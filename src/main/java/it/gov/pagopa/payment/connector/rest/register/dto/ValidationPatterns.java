package it.gov.pagopa.payment.connector.rest.register.dto;

public class ValidationPatterns {

    private ValidationPatterns(){}
    public static final String UUID_V4_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    public static final String OBJECT_ID_PATTERN = "^[a-fA-F0-9]{24}$";
    public static final String DIGITS_ONLY = "^\\d+$";
    public static final String GTIN_CODE = "^[a-zA-Z0-9]{1,14}$";
    public static final String ANY_TEXT = ".*";
    public static final String ROLE_PATTERN = "^(operatore|invitalia|invitalia_admin)$";
}