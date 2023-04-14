package it.gov.pagopa.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import it.gov.pagopa.payment.utils.Utils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it
                // https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                // region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
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

    @Autowired(required = false)
    private MongodExecutable embeddedMongoServer;

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @Autowired
    private WireMockServer wireMockServer;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
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
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        String mongoUrl;
        if (embeddedMongoServer != null) {
            Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
            mongoEmbeddedServerConfigField.setAccessible(true);
            MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
            Net mongodNet = Objects.requireNonNull(mongodConfig).net();

            mongoUrl = "mongodb://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort());
        } else {
            mongoUrl = mongodbUri.replaceFirst(":[^:]+(?=:[0-9]+)", "");
        }

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
