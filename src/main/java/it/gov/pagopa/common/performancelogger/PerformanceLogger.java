package it.gov.pagopa.common.performancelogger;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PerformanceLogger {

  private PerformanceLogger() {}

  public static <T> T execute(String flow, Supplier<T> logic) {
    long startTime = System.currentTimeMillis();
    try {
      return logic.get();
    } finally {
      log(flow, startTime);
    }
  }

  public static void log(String flow, long startTime){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        flow,
        System.currentTimeMillis() - startTime);
  }
}
