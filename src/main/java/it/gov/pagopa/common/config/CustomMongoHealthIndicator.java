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
       Document result = this.mongoTemplate.executeCommand("{ isMaster: 1 }");
       builder.up().withDetail("maxWireVersion", result.getInteger("maxWireVersion"));
       log.info("[MONGODB]: UP");
    }


}
