package it.gov.pagopa.payment.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.TimeZone;

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
}
