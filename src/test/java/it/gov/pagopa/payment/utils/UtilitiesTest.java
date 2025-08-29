package it.gov.pagopa.payment.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilitiesTest {

    @Test
    void testSanitizeString(){
        String trxCode = Utilities.sanitizeString("trx\nCode");

        assertEquals("trxCode", trxCode);
    }

}
