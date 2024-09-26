package it.gov.pagopa.common.config;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CustomMongoHealthIndicatorTest {

    @Test
    void testHealthCheckUp() throws Exception {

        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        CustomMongoHealthIndicator healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);

        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.runCommand(any(Document.class)))
                .thenReturn(new Document("ok", 1.0)); //Simulazione di un ping andato bene

        Health.Builder builder = new Health.Builder();
        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.up().withDetail("Ping result", "OK").build(), health);
    }

    @Test
    void testHealthCheckDown() throws Exception {

        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        CustomMongoHealthIndicator healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);

        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.runCommand(any(Document.class))).thenThrow(new RuntimeException("Database error"));

        Health.Builder builder = new Health.Builder();
        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.down().withException(new RuntimeException("Database error")).build(), health);
    }

    @Test
    void testHealthCheckPingFailed() throws Exception {

        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        CustomMongoHealthIndicator healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);

        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.runCommand(any(Document.class)))
                .thenReturn(new Document("ok", 0.0)); // Simulazione di un ping fallito

        Health.Builder builder = new Health.Builder();
        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Health.down().withDetail("Ping result", "Failed").build(), health);
    }
}
