package it.gov.pagopa.payment.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfigurationProperties {

    @Getter
    @Setter
    public static class ExtendedTransactions {
        int staleMinutesThreshold;
        int sendExpiredSendBatchSize;
        int updateBatchSize;
    }

}
