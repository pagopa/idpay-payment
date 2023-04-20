package it.gov.pagopa.common.performancelogger;

import static org.mockito.Mockito.when;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.MemoryAppender;
import it.gov.pagopa.payment.exception.ClientException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PerformanceLoggerAspect.class, PerformanceLoggerTest.TestService.class, PerformanceLoggerTest.DummyPayloadBuilder.class})
class PerformanceLoggerTest {

    @MockBean
    private Supplier<String> dummyServiceMock;

    @Autowired
    private TestService testService;

    private MemoryAppender memoryAppender;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PerformanceLogger.class.getName());
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    public static class TestService {
        @Autowired
        private Supplier<String> dummyService;

        @PerformanceLog("FLOWNAME1")
        public String annotatedMethod(){
            dummyService.get();
            return "ok1";
        }

        @PerformanceLog(value = "FLOWNAMEWITHPAYLOAD", payloadBuilderBeanClass = DummyPayloadBuilder.class)
        public String annotatedMethodWithPayloadBuilder(){
            return dummyService.get();
        }
    }

    public static class DummyPayloadBuilder implements PerformanceLoggerPayloadBuilder<String> {
        @Override
        public String apply(String s) {
            return "PAYLOADRETURNED: " + s;
        }
    }

    @Test
    void logSuccess() {
        when(dummyServiceMock.get())
                .thenReturn("OK");

        String result = testService.annotatedMethod();
        Assertions.assertEquals("ok1", result);

        assertPerformanceLogMessage("FLOWNAME1", "");
    }

    @Test
    void logSuccessWithPayload() {
        when(dummyServiceMock.get())
                .thenReturn("OK");

        String result = testService.annotatedMethodWithPayloadBuilder();
        Assertions.assertEquals("OK", result);

        assertPerformanceLogMessage("FLOWNAMEWITHPAYLOAD", "PAYLOADRETURNED: OK");
    }

    @Test
    void logSuccessWithPayloadWhenNull() {
        when(dummyServiceMock.get())
                .thenReturn(null);

        String result = testService.annotatedMethodWithPayloadBuilder();
        Assertions.assertNull(result);

        assertPerformanceLogMessage("FLOWNAMEWITHPAYLOAD", "Returned null");
    }

    @Test
    void logClientException() {
        ClientException expectedException = new ClientException(HttpStatus.NOT_FOUND, "DUMMYNOTFOUND");
        when(dummyServiceMock.get())
                .thenThrow(expectedException);

        try{
            testService.annotatedMethod();
            Assertions.fail("Expected exception");
        } catch (ClientException e){
            Assertions.assertEquals(expectedException, e);
        }

        assertPerformanceLogMessage("FLOWNAME1", "ClientException with HttpStatus 404 NOT_FOUND: DUMMYNOTFOUND");
    }

    @Test
    void logException() {
        IllegalStateException expectedException = new IllegalStateException("DUMMYILLEGALEXCEPTION");
        when(dummyServiceMock.get())
                .thenThrow(expectedException);

        try{
            testService.annotatedMethod();
            Assertions.fail("Expected exception");
        } catch (IllegalStateException e){
            Assertions.assertEquals(expectedException, e);
        }

        assertPerformanceLogMessage("FLOWNAME1", "Exception class java.lang.IllegalStateException: DUMMYILLEGALEXCEPTION");
    }

    private void assertPerformanceLogMessage(String expectedFlowName, String expectedPayload) {
        Assertions.assertEquals(1, memoryAppender.getLoggedEvents().size());
        String logMessage = memoryAppender.getLoggedEvents().get(0).getFormattedMessage();
        Assertions.assertTrue(
                logMessage.matches(
                        "\\[PERFORMANCE_LOG] \\[%s] Time occurred to perform business logic: \\d+ ms\\. .*".formatted(expectedFlowName)
                ) &&
                logMessage.endsWith(". " + expectedPayload),
                "Unexpected logged message: " + logMessage
        );
    }
}
