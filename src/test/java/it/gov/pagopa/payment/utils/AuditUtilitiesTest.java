package it.gov.pagopa.payment.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.AuditLogger;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditUtilitiesTest {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Payment dstip=%s", AuditLogger.SRCIP);
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String TRX_CODE = "TEST_TRX_CODE";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String CHANNEL = "TEST_CHANNEL";
    private static final String MERCHANT_ID = "TEST_MERCHANT_ID";
    public static final long REWARD = 0L;
    public static final String TRX_ID = "TEST_TRX_ID";
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
        auditUtilities.logCreatedTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, MERCHANT_ID);

        assertEquals(
                CEF + " msg=Transaction created"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s cs4Label=merchantId cs4=%s"
                                .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorCreateTransaction() {
        auditUtilities.logErrorCreatedTransaction(INITIATIVE_ID, MERCHANT_ID);

        assertEquals(
                CEF + " msg=Transaction created - KO"
                        + " cs1Label=initiativeId cs1=%s cs2Label=merchantId cs2=%s"
                        .formatted(INITIATIVE_ID, MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logRelatedUserToTransaction() {
        auditUtilities.logRelatedUserToTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL);

        assertEquals(
                CEF + " msg=User related to transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s cs4Label=channel cs4=%s suser=%s"
                                .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorRelatedUserToTransaction() {
        auditUtilities.logErrorRelatedUserToTransaction(TRX_CODE, USER_ID);

        assertEquals(
                CEF + " msg=User related to transaction - KO"
                        + " cs1Label=trxCode cs1=%s suser=%s"
                        .formatted(TRX_CODE, USER_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorPreviewTransaction() {
        auditUtilities.logErrorPreviewTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL);

        assertEquals(
                CEF + " msg=User request preview the transaction - KO"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s cs4Label=channel cs4=%s suser=%s"
                        .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logAuthorizedPayment() {
        auditUtilities.logAuthorizedPayment(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList());

        assertEquals(
                CEF + " msg=User authorized the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s suser=%s cs4Label=reward cs4=%s cs5Label=rejectionReasons cs5=%s"
                                .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, "[]"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorAuthorizedPayment() {
        auditUtilities.logErrorAuthorizedPayment(TRX_CODE, USER_ID);

        assertEquals(
                CEF + " msg=User authorized the transaction - KO"
                        + " cs1Label=trxCode cs1=%s suser=%s"
                        .formatted(TRX_CODE, USER_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logConfirmedPayment() {
        auditUtilities.logConfirmedPayment(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList(), MERCHANT_ID);


        assertEquals(
                CEF + " msg=Merchant confirmed the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s suser=%s cs4Label=reward cs4=%s cs5Label=rejectionReasons cs5=%s cs6Label=merchantId cs6=%s"
                        .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, "[]", MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorConfirmedPayment() {
        auditUtilities.logErrorConfirmedPayment(TRX_ID, MERCHANT_ID);


        assertEquals(
                CEF + " msg=Merchant confirmed the transaction - KO"
                        + " cs1Label=trxId cs1=%s cs2Label=merchantId cs2=%s"
                        .formatted(TRX_ID, MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logCancelTransaction() {
        auditUtilities.logCancelTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList(), MERCHANT_ID);


        assertEquals(
                CEF + " msg=Merchant cancelled the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s suser=%s cs4Label=reward cs4=%s cs5Label=rejectionReasons cs5=%s cs6Label=merchantId cs6=%s"
                        .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, "[]", MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorCancelTransaction() {
        auditUtilities.logErrorCancelTransaction(TRX_ID, MERCHANT_ID);


        assertEquals(
                CEF + " msg=Merchant cancelled the transaction - KO"
                        + " cs1Label=trxId cs1=%s cs2Label=merchantId cs2=%s"
                        .formatted(TRX_ID, MERCHANT_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logUnrelateTransaction() {
        auditUtilities.logUnrelateTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, Collections.emptyList());


        assertEquals(
                CEF + " msg=User unrelated the transaction"
                        + " cs1Label=initiativeId cs1=%s cs2Label=trxId cs2=%s cs3Label=trxCode cs3=%s suser=%s cs4Label=reward cs4=%s cs5Label=rejectionReasons cs5=%s"
                        .formatted(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, REWARD, "[]"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logErrorUnrelateTransaction() {
        auditUtilities.logErrorUnrelateTransaction(TRX_ID, USER_ID);


        assertEquals(
                CEF + " msg=User unrelated the transaction - KO"
                        + " cs1Label=trxId cs1=%s cs2Label=userId cs2=%s"
                        .formatted(TRX_ID, USER_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logDeleteTransactions() {
        auditUtilities.logDeleteTransactions(USER_ID, INITIATIVE_ID);


        assertEquals(
                CEF + " msg=Transactions in progress deleted"
                        + " suser=%s cs1Label=initiativeId cs1=%s"
                        .formatted(USER_ID, INITIATIVE_ID),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
