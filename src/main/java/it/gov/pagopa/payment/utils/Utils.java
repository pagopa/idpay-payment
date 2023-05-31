package it.gov.pagopa.payment.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    return euro != null ? euro.multiply(BigDecimal.valueOf(100)).longValue() : 0L;
  }

  public static Pageable getPageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, 10, Sort.by("updateDate"));
    }
    return pageable;
  }
}
