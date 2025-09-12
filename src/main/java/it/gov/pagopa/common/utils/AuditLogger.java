package it.gov.pagopa.common.utils;

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
        Object[] sanitizedParams = Arrays.stream(parameters).map(AuditLogger::sanitizeObject).toArray();
        log.info(pattern, sanitizedParams);
    }

    private static Object sanitizeObject(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj instanceof String str ?
                str.replaceAll("[\\r\\n]", " ") // Remove CR, LF
                        .replaceAll("[|{}]", "*") // |, {, }
                        .replaceAll("[^\\w\\s-\\[\\]]", "") // Only allow word, whitespace, -, [, ]
                : obj;
    }
}