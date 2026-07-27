package it.gov.pagopa.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;

public class CommonUtilities {
  private CommonUtilities() {}

  /** To convert cents into euro */
  public static BigDecimal centsToEuro(Long cents) {
    return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
  }

  public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  /** To convert euro into cents */
  public static Long euroToCents(BigDecimal euro){
    return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
  }

  public static Long minutesToSeconds(Integer minutes){
    return minutes == null ? null : (long)minutes*60;
  }

  public static Long secondsBetween(OffsetDateTime initialDate, OffsetDateTime endedDate) {
    if (initialDate == null || endedDate == null || endedDate.isBefore(initialDate)) {
      return null;
    }
    return Duration.between(initialDate, endedDate).getSeconds();
  }

  public static Long secondsBetween(LocalDateTime initialDate, LocalDateTime endedDate) {

    if (initialDate == null || endedDate == null || endedDate.isBefore(initialDate)) {
      return null;
    }

    ZoneId romeZone = ZoneId.of("Europe/Rome");
    ZonedDateTime initialZdt = initialDate.atZone(romeZone);
    ZonedDateTime endedZdt = endedDate.atZone(romeZone);

    return Duration.between(initialZdt, endedZdt).getSeconds();
  }
}
