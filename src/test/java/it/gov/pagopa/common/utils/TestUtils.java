package it.gov.pagopa.common.utils;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

public final class TestUtils {
    private TestUtils() {
    }

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(CommonConstants.ZONEID));
    }

    /**
     * applications's objectMapper
     */
    public static ObjectMapper objectMapper = new JsonConfig().objectMapper();

    /**
     * It will assert not null on all o's fields
     */
    public static void checkNotNullFields(Object o, String... excludedFields) {
        Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
        org.springframework.util.ReflectionUtils.doWithFields(o.getClass(),
                f -> {
                    f.setAccessible(true);
                    Assertions.assertNotNull(f.get(o), "The field %s of the input object of type %s is null!".formatted(f.getName(), o.getClass()));
                },
                f -> !excludedFieldsSet.contains(f.getName()));

    }

    /** it will create a BigDecimal with scale2 */
    public static BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * It will assert if 2 BigDecimal are equals, ignoring scale
     */
    public static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "Expected: %s, Obtained: %s".formatted(expected, actual));
    }

    /**
     * To serialize an object as a JSON handling Exception
     */
    public static String jsonSerializer(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * To deserialize an object as a JSON handling Exception
     */
    public static <T> T jsonDeserializer(String payload, Class<T> clazz) {
        try {
            return objectMapper.readValue(payload, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * To read {@link org.apache.kafka.common.header.Header} value
     */
    public static String getHeaderValue(ConsumerRecord<String, String> errorMessage, String errorMsgHeaderSrcServer) {
        Header header = errorMessage.headers().lastHeader(errorMsgHeaderSrcServer);
        return header!=null? new String(header.value()) : null;
    }

    /** it will attempt the test until its invocation successfully ends until the configured maxAttempts, waiting for the configured millis between each invocation */
    public static void waitFor(Callable<Boolean> test, Supplier<String> buildTestFailureMessage, int maxAttempts, int millisAttemptDelay) {
        try {
            await()
                    .pollInterval(millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .atMost((long) maxAttempts * millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .until(test);
        } catch (RuntimeException e) {
            Assertions.fail(buildTestFailureMessage.get(), e);
        }
    }

    /** To wait for the configured time */
    public static void wait(long timeout, TimeUnit timeoutUnit) {
        try{
            Awaitility.await().timeout(timeout, timeoutUnit).until(()->false);
        } catch (ConditionTimeoutException ex){
            // Do Nothing
        }
    }

    /** it will truncate the provided datetime field from payload */
    public static String truncateDateTimeField(String payload, String fieldName){
        return payload.replaceAll("(\""+fieldName+"\":\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}):[0-9]{2}:[0-9]{2}\\.?[0-9]*\"", "$1:--\"");
    }

    /** it will set to null the provided datetime field from payload */
    public static String setNullFieldValue(String payload, String fieldName) {
        return payload.replaceAll("(\""+fieldName+"\":)(?:[^,}]+)", "$1:null");
    }

    /** It will read a field value from json string */
    public static String readJsonStringFieldValue(String payload, String field) {
        int fieldIndex = payload.indexOf("\""+field+"\"");
        if(fieldIndex>-1){
            String afterField = payload.substring(fieldIndex+field.length()+2);
            final int afterOpeningQuote = afterField.indexOf('"') + 1;
            return afterField.substring(afterOpeningQuote, afterField.indexOf('"', afterOpeningQuote));
        }
        return null;
    }

    /** It will truncate timestamp value to MINUTES multiple of 10 */
    public static LocalDateTime truncateTimestamp(LocalDateTime timestamp) {
        return timestamp.truncatedTo(ChronoUnit.MINUTES).withMinute(timestamp.getMinute() / 10 * 10);
    }

    /** It will extract the response body and verify the HTTP status code */
    public static <T> T assertResponse(MvcResult response, HttpStatus expectedHttpStatusCode, Class<T> expectedBodyClass) {
        assertEquals(expectedHttpStatusCode.value(), response.getResponse().getStatus());
        if (expectedBodyClass != null) {
            try {
                return objectMapper.readValue(response.getResponse().getContentAsString(), expectedBodyClass);
            } catch (JsonProcessingException | UnsupportedEncodingException e) {
                throw new IllegalStateException("Cannot read body response!", e);
            }
        } else {
            return null;
        }
    }
}
