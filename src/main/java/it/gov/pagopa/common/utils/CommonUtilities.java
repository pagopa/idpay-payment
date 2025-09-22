package it.gov.pagopa.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

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

  public static Pageable getPageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, 10, Sort.by("updateDate").descending());
    }
    return pageable;
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
}
