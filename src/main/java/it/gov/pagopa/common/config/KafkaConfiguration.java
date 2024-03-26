package it.gov.pagopa.common.config;

import jakarta.annotation.PostConstruct;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.cloud.stream")
public class KafkaConfiguration {
    private Stream stream;
    @Data
    public static class Stream {
        private Map<String, KafkaInfoDTO> bindings;
        private Map<String,Binders> binders;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BaseKafkaInfoDTO {
        private String destination;
        private String group;
        private String type;
        private String brokers;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class KafkaInfoDTO extends BaseKafkaInfoDTO{
        private String binder;
    }
    @PostConstruct
    public void enrichKafkaInfoDTO(){
        stream.getBindings().forEach((k,v) -> {
            v.setType(getTypeForBinder(v.getBinder()));
            v.setBrokers(getBrokersForBinder(v.getBinder()));
        });
    }

    @Data
    public static class Binders{
        private String type;
        private Environment environment;
    }

    @Data
    public static class Environment {
        private Spring spring;
    }
    @Data
    public static class Spring{
        private Cloud cloud;
    }

    @Data
    public  static class Cloud{
        private StreamBinder stream;
    }

    @Data
    public static class StreamBinder{
        private Kafka kafka;
    }

    @Data
    public static class Kafka{
        private Binder binder;
    }

    @Data
    public static  class Binder{
        private String brokers;
    }

    public String getTypeForBinder(String binderName) {
        if (stream != null && stream.getBinders() != null) {
            Binders binders = stream.getBinders().get(binderName);
            if (binders != null) {
                return binders.getType();
            }
        }
        return null;
    }
    public String getBrokersForBinder(String binderName) {
        if (stream != null && stream.getBinders() != null) {
            Binders binders = stream.getBinders().get(binderName);
            if (binders != null) {
                return binders.getEnvironment().getSpring().getCloud().getStream().getKafka().getBinder().getBrokers();
            }
        }
        return null;
    }
}
