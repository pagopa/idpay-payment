package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants;


public class PdfGenerationException extends ServiceException {

    public PdfGenerationException(String message, boolean printStackTrace, Throwable ex) {
        this(PaymentConstants.ExceptionCode.PDF_GENERIC_EXCEPTION, message, printStackTrace, ex);
    }
    public PdfGenerationException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message,printStackTrace, ex);
    }
}
