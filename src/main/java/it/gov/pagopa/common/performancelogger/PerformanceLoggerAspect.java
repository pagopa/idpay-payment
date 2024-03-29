package it.gov.pagopa.common.performancelogger;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@Aspect
@Slf4j
public class PerformanceLoggerAspect {

    private final ApplicationContext applicationContext;

    public PerformanceLoggerAspect(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(performanceLog)")
    public Object performanceLogger(ProceedingJoinPoint pjp, PerformanceLog performanceLog) {
        return PerformanceLogger.execute(performanceLog.value(), () -> {
            try {
                return pjp.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException("Something went wrong while executing PerformanceLog annotated method", e);
            }
        }, getPayloadBuilder(performanceLog));
    }

    private Function<Object, String> getPayloadBuilder(PerformanceLog performanceLog) {
        //noinspection unchecked
        return performanceLog.payloadBuilderBeanClass() != PerformanceLoggerPayloadBuilder.class
                ? applicationContext.getBean(performanceLog.payloadBuilderBeanClass())
                : null;
    }
}
