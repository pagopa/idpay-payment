package it.gov.pagopa.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.payment.utils.Utils;
import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
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
        })
@AutoConfigureMockMvc
@AutoConfigureWireMock(stubs = "classpath:/stub", port = 0)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private MongoProperties mongoProperties;

    @Autowired
    private WireMockServer wireMockServer;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        // At the start of the Spring Context, the TimeZone applied is defined in the configuration properties of maven-surefire-plugin inside the pom.xml
        TimeZone.setDefault(TimeZone.getTimeZone(Utils.ZONEID));

        unregisterMBean("kafka.*:*");
        unregisterMBean("org.springframework.*:*");
    }

    private static void unregisterMBean(String objectName) throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName mbeanName = new ObjectName(objectName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectInstance mBean : mBeanServer.queryMBeans(mbeanName, null)) {
            mBeanServer.unregisterMBean(mBean.getObjectName());
        }
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        String mongoUrl = mongoProperties.getUri().replaceFirst("(?<=//)[^@]+@", "");

        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Wiremock HTTP: http://localhost:%s
                        Wiremock HTTPS: %s
                        ************************
                        """,
                mongoUrl,
                wireMockServer.getOptions().portNumber(),
                wireMockServer.baseUrl());
    }

    protected static void wait(long timeout, TimeUnit timeoutUnit) {
        try {
            Awaitility.await().timeout(timeout, timeoutUnit).until(() -> false);
        } catch (ConditionTimeoutException ex) {
            // Do Nothing
        }
    }
}
