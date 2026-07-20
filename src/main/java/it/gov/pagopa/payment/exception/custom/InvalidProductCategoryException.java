package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.INVALID_PRODUCT_CATEGORY;

public class InvalidProductCategoryException extends ServiceException {

    public InvalidProductCategoryException(String message) {
        this(INVALID_PRODUCT_CATEGORY, message);
    }

    public InvalidProductCategoryException(String code, String message) {
        this(code, message, false, null);
    }

    public InvalidProductCategoryException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}