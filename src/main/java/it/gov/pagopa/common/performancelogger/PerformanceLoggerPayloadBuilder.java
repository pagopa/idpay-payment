package it.gov.pagopa.common.performancelogger;

import java.util.function.Function;

public interface PerformanceLoggerPayloadBuilder<T> extends Function<T, String> {
}
