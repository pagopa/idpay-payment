package it.gov.pagopa.payment.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;

public class Utils {
  private Utils() {}

  public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

  /** To convert cents into euro */
  public static BigDecimal centsToEuro(Long cents) {
    return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
  }

  /** To convert euro into cents */
  public static Long euroToCents(BigDecimal euro) {
    return euro.multiply(BigDecimal.valueOf(100)).longValue();
  }
}
