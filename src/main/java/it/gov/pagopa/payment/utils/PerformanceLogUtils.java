package it.gov.pagopa.payment.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PerformanceLogUtils {

  private PerformanceLogUtils() {}

  public static void performanceLog(String flow, Long startTime){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        flow,
        System.currentTimeMillis() - startTime);
  }
}
