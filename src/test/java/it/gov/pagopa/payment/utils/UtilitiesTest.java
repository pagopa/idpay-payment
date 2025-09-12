package it.gov.pagopa.payment.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilitiesTest {

    @Test
    void testSanitizeString() {
        assertEquals(null, Utilities.sanitizeString(null));
        assertEquals("trxCode", Utilities.sanitizeString("trx\nCode"));
        assertEquals("trxCode2", Utilities.sanitizeString("trx\rCode2"));
        assertEquals("123-aaf-555", Utilities.sanitizeString("123-aaf-555"));
        assertEquals("123_aaf_555", Utilities.sanitizeString("123_aaf_555"));
        assertEquals("key value", Utilities.sanitizeString("key [value]"));
    }

}
