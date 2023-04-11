package it.gov.pagopa.common.performancelogger;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.MemoryAppender;
import it.gov.pagopa.payment.controller.QRCodePaymentControllerImpl;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.exception.ErrorManager;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {PerformanceLoggerAspect.class, QRCodePaymentControllerImpl.class, ErrorManager.class})
@WebMvcTest(QRCodePaymentControllerImpl.class)
class PerformanceLoggerTest {

    @MockBean
    private QRCodePaymentService qrCodePaymentService;

    @Autowired
    private MockMvc mockMvc;

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

    private ResultActions callRestApi() throws Exception {
        return mockMvc
                .perform(
                        post("/idpay/payment/qr-code/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"));
    }

    @Test
    void logSuccess() throws Exception {
        when(qrCodePaymentService.createTransaction(any()))
                .thenReturn(new TransactionResponse());

        callRestApi()
                .andExpect(status().is2xxSuccessful());

        assertPerformanceLogMessage();
    }

    @Test
    void logException() throws Exception {
        when(qrCodePaymentService.createTransaction(any()))
                .thenThrow(new IllegalStateException());

        callRestApi()
                .andExpect(status().is5xxServerError())
                .andReturn();

        assertPerformanceLogMessage();
    }

    private void assertPerformanceLogMessage() {
        Assertions.assertEquals(1, memoryAppender.getLoggedEvents().size());
        String logMessage = memoryAppender.getLoggedEvents().get(0).getFormattedMessage();
        Assertions.assertTrue(
                logMessage.matches(
                        "\\[PERFORMANCE_LOG] \\[CREATE_TRANSACTION_QR_CODE] Time occurred to perform business logic: \\d+ ms"
                ),
                "Unexpected logged message: " + logMessage
        );
    }
}
