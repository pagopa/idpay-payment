package it.gov.pagopa.common.performancelogger;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@Aspect
@Slf4j
public class PerformanceLoggerAspect {

    @Around("@annotation(performanceLog)")
    public Object performanceLogger(ProceedingJoinPoint pjp, PerformanceLog performanceLog) {
        return PerformanceLogger.execute(performanceLog.value(), () -> {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                throw new IllegalStateException("Something gone wrong while executing PerformanceLog annotated method", e);
            }
        });
    }
}
