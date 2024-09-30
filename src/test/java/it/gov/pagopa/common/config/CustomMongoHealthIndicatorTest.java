package it.gov.pagopa.common.config;

import it.gov.pagopa.common.mongo.config.MongoHealthConfig;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MongoHealthConfig.class})
class CustomMongoHealthIndicatorTest {

        @Autowired
        private CustomMongoHealthIndicator customMongoHealthIndicator;

        @MockBean
        private MongoTemplate mongoTemplate;

    @Test
    void testHealthUp() throws Exception {
        Document pingResult = new Document("ok", 1.0);
        when(mongoTemplate.executeCommand(new Document("ping", 1))).thenReturn(pingResult);

        Health.Builder builder = new Health.Builder();
        customMongoHealthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.up().withDetail("pingResult", pingResult).build(), health);
    }

    @Test
    void testHealthDownPingFailed() throws Exception {
        Document pingResult = new Document("ok", 0.0);
        when(mongoTemplate.executeCommand(new Document("ping", 1))).thenReturn(pingResult);

        Health.Builder builder = new Health.Builder();
        customMongoHealthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.down().withDetail("pingResult", pingResult).withDetail("error", "Ping failed").build(), health);
    }

    @Test
    void testHealthDownException() throws Exception {
        when(mongoTemplate.executeCommand(new Document("ping", 1))).thenThrow(new RuntimeException("Connection error"));

        Health.Builder builder = new Health.Builder();
        customMongoHealthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.down().withDetail("error", "Exception occurred: Connection error").build(), health);
    }
    }
