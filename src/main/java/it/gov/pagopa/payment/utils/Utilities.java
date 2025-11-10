package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.InvalidInvoiceFormatException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public final class Utilities {
    private Utilities() {}

    public static String sanitizeString(String str){
        if(str == null) {
            return null;
        }
        return str.replaceAll("[\\r\\n]", "") // remove new line and carriage return
                .replaceAll("[^\\w\\s-]", ""); // allow only alphanumeric, whitespace, dash
    }

    public static LocalDate getLocalDate(OffsetDateTime date) {
        return date.toInstant()
                .atZone(TimeZone.getDefault().toZoneId())
                .toLocalDate();
    }

    public static void checkFileExtensionOrThrow(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null ||
            (!file.getOriginalFilename().toLowerCase().endsWith(".pdf") &&
                !file.getOriginalFilename().toLowerCase().endsWith(".xml"))) {
            throw new InvalidInvoiceFormatException(ExceptionCode.GENERIC_ERROR, "File must be a PDF or XML");
        }
    }

    public static void checkDocumentNumberOrThrow(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new InvalidInvoiceFormatException(ExceptionCode.GENERIC_ERROR, "Document number is required");
        }
    }
}
