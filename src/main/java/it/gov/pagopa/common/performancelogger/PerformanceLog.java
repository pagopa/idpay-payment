package it.gov.pagopa.common.performancelogger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PerformanceLog {
    /** Performance log's flow name*/
    String value();
    /** The optional bean class searched inside Spring context in order to build a payload to concatenate to performance log. */
    Class<? extends PerformanceLoggerPayloadBuilder> payloadBuilderBeanClass() default PerformanceLoggerPayloadBuilder.class;
}
