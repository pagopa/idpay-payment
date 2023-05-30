package it.gov.pagopa.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.transactionOutcome-out-0.destination}"
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it
                // https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                // region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.de.flapdoodle.embed.mongo.spring.autoconfigure=WARN",
                "de.flapdoodle.mongodb.embedded.version=4.0.21",
                // endregion


                //region wiremock
                "logging.level.WireMock=ERROR",
                "rest-client.reward.baseUrl=http://localhost:${wiremock.server.port}",
                //endregion

                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.transaction-outcome.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-errors.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}"
                //endregion
        })
@AutoConfigureMockMvc
@AutoConfigureWireMock(stubs = "classpath:/stub", port = 0)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected KafkaTestUtilitiesService kafkaTestUtilitiesService;
    @Autowired
    private MongoTestUtilitiesService mongoTestUtilitiesService;

    @Autowired
    private WireMockServer wireMockServer;

    @Value("${spring.cloud.stream.bindings.errors-out-0.destination}")
    protected String topicErrors;
    @Value("${spring.cloud.stream.bindings.transactionOutcome-out-0.destination}")
    protected String topicConfirmNotification;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Wiremock HTTP: http://localhost:%s
                        Wiremock HTTPS: %s
                        Embedded kafka: %s
                        ************************
                        """,
                mongoTestUtilitiesService.getMongoUrl(),
                wireMockServer.getOptions().portNumber(),
                wireMockServer.baseUrl(),
                kafkaTestUtilitiesService.getKafkaUrls());
    }

    protected static void wait(long timeout, TimeUnit timeoutUnit) {
        try {
            Awaitility.await().timeout(timeout, timeoutUnit).until(() -> false);
        } catch (ConditionTimeoutException ex) {
            // Do Nothing
        }
    }

//region kafka utility methods
    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String expectedPayload) {
        if(expectedPayload!=null) {
            String temp = TestUtils.truncateDateTimeField(expectedPayload, "elaborationDateTime");
            return TestUtils.truncateDateTimeField(temp, "timestamp");
        } else {
            return null;
        }
    }
//endregion
}
