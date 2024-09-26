package it.gov.pagopa.common.config;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class CustomMongoHealthIndicator extends MongoHealthIndicator {

    private final MongoTemplate mongoTemplate;

    public CustomMongoHealthIndicator(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            MongoDatabase database = mongoTemplate.getDb();
            // Esecuzione del comando ping che verificherà l'effettiva risposta dal db
            Document result = database.runCommand(new Document("ping", 1));

            if (result.getDouble("ok") == 1.0) {
                builder.up().withDetail("Ping result", "OK");
                log.info("[HEALTH MONGODB - UP] Ping result: OK"+ new Date());

            } else {
                builder.down().withDetail("Ping result", "Failed");
                log.error("[HEALTH MONGODB - DOWN] Ping result: Failed");
            }
        } catch (Exception e) {
            builder.down(e);
            log.error("[HEALTH MONGODB - DOWN] Ping result: Failed");
        }
    }
}