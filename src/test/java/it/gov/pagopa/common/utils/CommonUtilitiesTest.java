package it.gov.pagopa.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Slf4j
class CommonUtilitiesTest {

    @Test
    void testCentsToEuro(){
        Assertions.assertEquals(
                BigDecimal.valueOf(5).setScale(2, RoundingMode.UNNECESSARY),
                CommonUtilities.centsToEuro(5_00L)
        );
    }

    @Test
    void testEuroToCents(){
        Assertions.assertNull(CommonUtilities.euroToCents(null));
        Assertions.assertEquals(100L, CommonUtilities.euroToCents(BigDecimal.ONE));
        Assertions.assertEquals(325L, CommonUtilities.euroToCents(BigDecimal.valueOf(3.25)));

        Assertions.assertEquals(
                5_00L,
                CommonUtilities.euroToCents(TestUtils.bigDecimalValue(5))
        );
    }

    @Test
    void testMinutesToSeconds(){
        Assertions.assertNull(CommonUtilities.minutesToSeconds(null));
        Assertions.assertEquals(300 ,CommonUtilities.minutesToSeconds(5));
    }

    @Test
    void testSecondsBetween(){
        OffsetDateTime now = OffsetDateTime.now();
        Assertions.assertNull(CommonUtilities.secondsBetween(null, null));
        Assertions.assertNull(CommonUtilities.secondsBetween(null, now));
        Assertions.assertNull(CommonUtilities.secondsBetween(now, null));
        Assertions.assertNull(CommonUtilities.secondsBetween(now, now.minusMinutes(2)));
        Assertions.assertEquals(0, CommonUtilities.secondsBetween(now, now));
        Assertions.assertEquals(300, CommonUtilities.secondsBetween(now, now.plusMinutes(5)));
    }
}
