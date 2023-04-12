package it.gov.pagopa.payment.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
  private Utils() {}

  /** To convert cents into euro */
  public static BigDecimal centsToEuro(Long cents) {
    return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
  }
}
