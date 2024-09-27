package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomMongoHealthIndicator;
import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoHealthConfig {
    @Bean
    public MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new CustomMongoHealthIndicator(mongoTemplate);
    }
}

