package it.gov.pagopa.payment;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;

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
    })
public abstract class BaseIntegrationTest {

    @Autowired(required = false)
    private MongodExecutable embeddedMongoServer;

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @PostConstruct
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        String mongoUrl;
        if(embeddedMongoServer != null) {
            Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
            mongoEmbeddedServerConfigField.setAccessible(true);
            MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
            Net mongodNet = Objects.requireNonNull(mongodConfig).net();

            mongoUrl="mongodb://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort());
        } else {
            mongoUrl=mongodbUri.replaceFirst(":[^:]+(?=:[0-9]+)", "");
        }

        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        ************************
                        """,
            mongoUrl);
    }

}
