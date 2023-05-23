package it.gov.pagopa.payment.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditUtilitiesTest {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s", AuditUtilities.SRCIP);
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String TRX_CODE = "TEST_TRX_CODE";
    private static final String USER_ID = "TEST_USER_ID";
    public static final long REWARD = 0L;
    private final AuditUtilities auditUtilities = new AuditUtilities();
    private MemoryAppender memoryAppender;


    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void logCreateTransaction() {
        auditUtilities.logCreatedTransaction(INITIATIVE_ID, TRX_CODE);

        assertEquals(
                CEF + " msg=Transaction created"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxCode cs2=%s"
                                .formatted(INITIATIVE_ID, TRX_CODE),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logRelatedUserToTransaction() {
        auditUtilities.logRelatedUserToTransaction(INITIATIVE_ID, TRX_CODE, USER_ID);

        assertEquals(
                CEF + " msg=User related to transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxCode cs2=%s suser=%s"
                                .formatted(INITIATIVE_ID, TRX_CODE, USER_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logAuthorizedPayment() {
        auditUtilities.logAuthorizedPayment(INITIATIVE_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList());

        assertEquals(
                CEF + " msg=User authorized the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxCode cs2=%s suser=%s cs3Label=reward cs3=%s cs4Label=rejectionReasons cs4=%s"
                                .formatted(INITIATIVE_ID, TRX_CODE, USER_ID, REWARD, "[]"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logConfirmedPayment() {
        auditUtilities.logConfirmedPayment(INITIATIVE_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList());


        assertEquals(
                CEF + " msg=Merchant confirmed the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxCode cs2=%s suser=%s cs3Label=reward cs3=%s cs4Label=rejectionReasons cs4=%s"
                        .formatted(INITIATIVE_ID, TRX_CODE, USER_ID, REWARD, "[]"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
