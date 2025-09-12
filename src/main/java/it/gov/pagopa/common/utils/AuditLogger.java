package it.gov.pagopa.common.utils;

import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@Slf4j(topic = "AUDIT")
public class AuditLogger {
    private AuditLogger() {
    }

    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp = "UNKNOWN";
        }

        SRCIP = srcIp;
    }

    public static void logAuditString(String pattern, String... parameters) {
        Object[] sanitizedParams = Arrays.stream(parameters).map(AuditLogger::removeLogInjectionChars).toArray();
        log.info(pattern, sanitizedParams);
    }

    /**
     * Removes newline and carriage return characters to prevent log injection.
     */
    private static String removeLogInjectionChars(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\r\\n]", "");
    }
}