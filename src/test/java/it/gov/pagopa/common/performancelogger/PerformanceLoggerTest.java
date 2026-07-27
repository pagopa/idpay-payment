package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.common.web.exception.ClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceLoggerTest {

    private static final String FLOW = "TEST_FLOW";

    @Test
    void testExecute_Success_NullPayloadBuilder() {
        // Given & When
        String result = PerformanceLogger.execute(FLOW, () -> "SUCCESS", null);

        // Then
        assertEquals("SUCCESS", result);
    }

    @Test
    void testExecute_Success_WithPayloadBuilder() {
        // Given
        Function<String, String> payloadBuilder = res -> "Payload: " + res;

        // When
        String result = PerformanceLogger.execute(FLOW, () -> "SUCCESS", payloadBuilder);

        // Then
        assertEquals("SUCCESS", result);
    }

    @Test
    void testExecute_Success_NullOutput() {
        // Given
        Function<String, String> payloadBuilder = res -> "Payload: " + res;

        // When
        String result = PerformanceLogger.execute(FLOW, () -> null, payloadBuilder);

        // Then
        assertNull(result);
    }

    @Test
    void testExecute_PayloadBuilderThrowsException() {
        // Given
        Function<String, String> payloadBuilder = res -> {
            throw new RuntimeException("Payload builder error");
        };

        // When
        String result = PerformanceLogger.execute(FLOW, () -> "SUCCESS", payloadBuilder);

        // Then
        assertEquals("SUCCESS", result);
    }

    @Test
    void testExecute_ClientExceptionThrown() {
        // Given
        ClientException clientException = new ClientException(HttpStatus.BAD_REQUEST, "Invalid input");

        // When & Then
        ClientException thrown = assertThrows(
                ClientException.class,
                () -> PerformanceLogger.execute(FLOW, () -> {
                    throw clientException;
                }, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getHttpStatus());
        assertEquals("Invalid input", thrown.getMessage());
    }

    @Test
    void testExecute_GenericExceptionThrown() {
        // Given
        RuntimeException genericException = new RuntimeException("Unexpected error");

        // When & Then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> PerformanceLogger.execute(FLOW, () -> {
                    throw genericException;
                }, null)
        );

        assertEquals("Unexpected error", thrown.getMessage());
    }

    @Test
    void testLogDirectCall() {
        // Given
        long startTime = System.currentTimeMillis() - 100;
        String payload = "Direct log payload";

        // When & Then
        assertDoesNotThrow(() -> PerformanceLogger.log(FLOW, startTime, payload));
    }
}