package it.gov.pagopa.payment.event.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {

    public static final String TIMEOUT_CONSUMER_BINDING_NAME = "paymentTimeoutConsumer-in-0";
    private boolean contextReady = false;
    private Binding<?> paymentTimeoutConsumerBinding;
    @Bean
    public Consumer<String> paymentTimeoutConsumer() { 	return a -> log.info(a + "[TIMEOUT-CONSUMER-TEST]"); 	}

    @EventListener(BindingCreatedEvent.class)
    public void onBindingCreatedEvent(BindingCreatedEvent event) {
        if (event.getSource() instanceof Binding<?> binding && TIMEOUT_CONSUMER_BINDING_NAME.equals(binding.getBindingName())) {
            paymentTimeoutConsumerBinding = binding;

            if (contextReady) {
                log.info("[BENEFICIARY_CONTEXT_START] Application started and context ready");
                synchronized (this) {
                    makeServiceBusBindingRestartable(binding);
                    binding.start();
                }
            } else {
                log.info("[BENEFICIARY_CONTEXT_START] Application started but context not ready");
            }
        }
    }

    /*
     * Only setting "group" property makes the binding restartable.
     * We are using a queue, and group is a configuration valid just for topics, so we cannot configure it.
     * Because we are setting the auto-startup to false, we are not more able to start it when the container is ready without changing this flag
     */
    @SuppressWarnings("squid:S3011") // suppressing reflection accesses
    private static void makeServiceBusBindingRestartable(Binding<?> binding) {
        try {
            Field restartableField = ReflectionUtils.findField(binding.getClass(), "restartable");
            if (restartableField == null) {
                throw new IllegalStateException("Cannot make servicebus binding restartable");
            }

            restartableField.setAccessible(true);
            restartableField.setBoolean(binding, true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot make servicebus binding restartable", e);
        }
    }
}
