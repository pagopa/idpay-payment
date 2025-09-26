package it.gov.pagopa.payment.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class UtilitiesTest {

    private TimeZone previousTz;

    @BeforeEach
    void saveTz() {
        previousTz = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTz() {
        TimeZone.setDefault(previousTz);
    }

    @Test
    void testSanitizeString() {
        assertNull(null, Utilities.sanitizeString(null));
        assertEquals("trxCode", Utilities.sanitizeString("trx\nCode"));
        assertEquals("trxCode2", Utilities.sanitizeString("trx\rCode2"));
        assertEquals("123-aaf-555", Utilities.sanitizeString("123-aaf-555"));
        assertEquals("123_aaf_555", Utilities.sanitizeString("123_aaf_555"));
        assertEquals("key value", Utilities.sanitizeString("key [value]"));
    }

    @Test
    void getLocalDate_sameDay_inEuropeRome_forMiddayUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        OffsetDateTime odt = OffsetDateTime.of(2025, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);

        LocalDate result = Utilities.getLocalDate(odt);

        assertEquals(LocalDate.of(2025, 1, 10), result);
    }

    @Test
    void getLocalDate_rollsToNextDay_inEuropeRome_forLateEveningUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        OffsetDateTime odt = OffsetDateTime.of(2025, 1, 10, 23, 30, 0, 0, ZoneOffset.UTC);

        LocalDate result = Utilities.getLocalDate(odt);

        assertEquals(LocalDate.of(2025, 1, 11), result);
    }

    @Test
    void getLocalDate_dependsOnSystemTimeZone() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 1, 10, 23, 30, 0, 0, ZoneOffset.UTC);

        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        LocalDate ny = Utilities.getLocalDate(odt);

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        LocalDate rome = Utilities.getLocalDate(odt);

        assertEquals(LocalDate.of(2025, 1, 10), ny);
        assertEquals(LocalDate.of(2025, 1, 11), rome);
        assertNotEquals(ny, rome);
    }
}
