package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomMongoHealthIndicator;
import it.gov.pagopa.common.config.HealthIndicatorLogger;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

@Configuration
public class MongoHealthConfig    {
    @Bean
    public CustomMongoHealthIndicator customMongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new CustomMongoHealthIndicator(mongoTemplate);
    }

    @Bean
    public HealthIndicatorLogger healthIndicatorLogger(List<HealthIndicator> healthIndicatorList) {
        // Assicurati che non ci sia duplicazione della classe CustomMongoHealthIndicator
        healthIndicatorList.removeIf(this::isCustomMongoHealthIndicator);
        return new HealthIndicatorLogger(healthIndicatorList);
    }

    // Method to check if an indicator is an instance of CustomMongoHealthIndicator
    private boolean isCustomMongoHealthIndicator(HealthIndicator indicator) {
        return indicator instanceof CustomMongoHealthIndicator;
    }
}

