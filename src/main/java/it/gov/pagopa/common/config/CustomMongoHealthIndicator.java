package it.gov.pagopa.common.config;

import org.bson.Document;
import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;


public class CustomMongoHealthIndicator extends MongoHealthIndicator {

    private final MongoTemplate mongoTemplate;

    public CustomMongoHealthIndicator(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            Document pingResult = mongoTemplate.executeCommand(new Document("ping", 1));
            Double okValue = pingResult.getDouble("ok");
            if (okValue != null && okValue.equals(1.0)) {
                builder.up().withDetail("pingResult", pingResult);
            } else {
                builder.down().withDetail("pingResult", pingResult).withDetail("error", "Ping failed");
            }
        } catch (Exception e) {
            builder.down().withDetail("error", "Exception occurred: " + e.getMessage());
        }
    }


}
