package it.gov.pagopa.payment.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfigurationProperties {

    @Configuration
    @ConfigurationProperties(prefix = "app.extended-transactions")
    @Data
    public static class ExtendedTransactions {
        int staleMinutesThreshold;
        int sendExpiredSendBatchSize;
        int updateBatchSize;
    }

}
