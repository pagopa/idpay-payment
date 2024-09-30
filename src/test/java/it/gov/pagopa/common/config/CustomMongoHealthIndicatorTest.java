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
        void testHealthCheckUp() throws Exception {
            Document pingResult = new Document("ok", 1.0);
            when(mongoTemplate.executeCommand(new Document("ping", 1))).thenReturn(pingResult);

            Health.Builder builder = new Health.Builder();
            customMongoHealthIndicator.doHealthCheck(builder);

            Health health = builder.build();
            assertEquals(Health.up().withDetail("pingResult", pingResult).build(), health);
        }

        @Test
        void testHealthCheckDownInvalidOk() throws Exception {
            Document pingResult = new Document("ok", 0.0);
            when(mongoTemplate.executeCommand(new Document("ping", 1))).thenReturn(pingResult);

            Health.Builder builder = new Health.Builder();
            customMongoHealthIndicator.doHealthCheck(builder);

            Health health = builder.build();
            assertEquals(Health.down().build(), health);
        }

        @Test
        void testHealthCheckDownEmptyPingResult() throws Exception {
            Document pingResult = new Document();
            when(mongoTemplate.executeCommand(new Document("ping", 1))).thenReturn(pingResult);

            Health.Builder builder = new Health.Builder();
            customMongoHealthIndicator.doHealthCheck(builder);

            Health health = builder.build();
            assertEquals(Health.down().build(), health);
        }

        @Test
        void testHealthCheckDownException() throws Exception {
            when(mongoTemplate.executeCommand(new Document("ping", 1))).thenThrow(new RuntimeException("Connection error"));

            Health.Builder builder = new Health.Builder();
            customMongoHealthIndicator.doHealthCheck(builder);

            Health health = builder.build();
            assertEquals(Health.down().build(), health);
        }
    }
