package it.gov.pagopa.common.config;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
public class CustomMongoHealthIndicator extends AbstractHealthIndicator {

    private final MongoTemplate mongoTemplate;
    private Health cachedHealth = null;
    private long lastCheckTime = 0;
    private static final long CACHE_TTL_MS = 10000; // Cache valida per 10 secondi

    public CustomMongoHealthIndicator(MongoTemplate mongoTemplate) {
        super("MongoDB health check failed");
        Assert.notNull(mongoTemplate, "MongoTemplate must not be null");
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        long currentTime = System.currentTimeMillis();

   /*     // Restituisci risultato cache se non scaduto
        if (cachedHealth != null && (currentTime - lastCheckTime) < CACHE_TTL_MS) {
            builder.status(cachedHealth.getStatus()).withDetails(cachedHealth.getDetails());
            log.debug("[HEALTH MONGODB - CACHED] Restituito risultato dalla cache.");
            return;
        } */

        // Esegui controllo Mongo e aggiorna cache
        lastCheckTime = currentTime;
        try {
            MongoDatabase database = mongoTemplate.getDb();
            Document result = database.runCommand(new Document("ping", 1));

            if (result.getDouble("ok") == 1.0) {
                builder.up().withDetail("Ping result", "OK");
                log.info("[HEALTH MONGODB - UP] Ping result: OK");
            } else {
                builder.down().withDetail("Ping result", "Failed");
                log.error("[HEALTH MONGODB - DOWN] Ping result: Failed");
            }

        } catch (Exception e) {
            builder.down(e);
        }

        // Aggiorna la cache con il nuovo risultato
        cachedHealth = builder.build();
    }
}
