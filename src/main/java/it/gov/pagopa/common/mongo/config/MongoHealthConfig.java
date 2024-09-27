package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomMongoHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoHealthConfig {
    @Bean
    @Primary
    public AbstractHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new CustomMongoHealthIndicator(mongoTemplate);
    }
}

