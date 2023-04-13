package it.gov.pagopa.common.performancelogger;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.function.Function;

@Configuration
@EnableAspectJAutoProxy
@Aspect
@Slf4j
public class PerformanceLoggerAspect {

    @Autowired
    private ApplicationContext applicationContext;

    @Around("@annotation(performanceLog)")
    public Object performanceLogger(ProceedingJoinPoint pjp, PerformanceLog performanceLog) {
        return PerformanceLogger.execute(performanceLog.value(), () -> {
            try {
                return pjp.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException("Something gone wrong while executing PerformanceLog annotated method", e);
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
