package it.gov.pagopa.payment.configuration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageSchedulerConfig {

    @Value("${spring.cloud.azure.servicebus-ns-manager.connection-string}")
    private String connectionString;

    @Value("${spring.cloud.azure.servicebus-ns-manager.queue-name}")
    private String queueName;

    @Bean
    public ServiceBusSenderClient messageScheduler() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

}