package it.gov.pagopa.payment.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilitiesTest {

    @Test
    void testSanitizeString(){
        String trxCode = Utilities.sanitizeString("trx\nCode");

        assertEquals("trxCode", trxCode);
    }

}
