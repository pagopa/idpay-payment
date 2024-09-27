package it.gov.pagopa.common.config;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;

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
            // Esecuzione del comando ping
            Document pingResult = mongoTemplate.executeCommand(new Document("ping", 1));

            // Controlla se il ping ha avuto successo
            if (!pingResult.isEmpty()) {
                // Verifica se il campo "ok" è presente e uguale a 1.0
                Double okValue = pingResult.getDouble("ok");
                if (okValue != null && okValue.equals(1.0)) {
                    builder.up().withDetail("pingResult", pingResult);
                    log.info("Ping OK: {}", pingResult);
                } else {
                    log.error("Ping failed: {}", pingResult);
                    builder.down();
                }
            } else {
                log.error("Ping returned null.");
                builder.down();
            }
        } catch (Exception e) {
            log.error("Error executing ping command: {}", e.getMessage());
            builder.down();
        }
    }

}
