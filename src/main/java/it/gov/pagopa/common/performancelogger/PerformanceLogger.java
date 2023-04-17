package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.exception.ClientException;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PerformanceLogger {

  private PerformanceLogger() {}

  public static <T> T execute(String flow, Supplier<T> logic, Function<T, String> payloadBuilder) {
    long startTime = System.currentTimeMillis();
    String payload = "";
    try {
      T out = logic.get();
      payload = buildPayload(out, payloadBuilder);
      return out;
    } catch (ClientException clientException) {
      payload = "ClientException with HttpStatus %s: %s".formatted(clientException.getHttpStatus(), clientException.getMessage());
      throw clientException;
    } catch (Exception e){
      payload = "Exception %s: %s".formatted(e.getClass(), e.getMessage());
      throw e;
    }
    finally {
      log(flow, startTime, payload);
    }
  }

  private static <T> String buildPayload(T out, Function<T, String> payloadBuilder) {
    String payload;
    if(payloadBuilder!=null) {
      if (out != null) {
        try{
          payload = payloadBuilder.apply(out);
        } catch (Exception e){
          log.warn("Something gone wrong while building payload", e);
          payload = "Payload builder thrown Exception %s: %s".formatted(e.getClass(), e.getMessage());
        }
      } else {
        payload = "Returned null";
      }
    } else {
      payload = "";
    }
    return payload;
  }

  public static void log(String flow, long startTime, String payload){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms. {}",
        flow,
        System.currentTimeMillis() - startTime,
        payload);
  }
}
