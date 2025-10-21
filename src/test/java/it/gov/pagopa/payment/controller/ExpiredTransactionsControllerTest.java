package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.ExpiredTransactionsProcessedDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.service.payment.TransactionInProgressService;
import org.apache.http.util.Asserts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static reactor.core.publisher.Mono.when;

@WebMvcTest(ExpiredTransactionsController.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class})
public class ExpiredTransactionsControllerTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionInProgressService transactionInProgressServiceMock;

    @Test
    void shouldReturnUpdatedTransactionNumberOnValidRequest() throws Exception {
        Mockito.when(transactionInProgressServiceMock.findAndUpdateExpiredTransactionsStatus(eq(INITIATIVE_ID)))
                .thenReturn(1L);
        MvcResult result = mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/update-status",
                        INITIATIVE_ID)
        ).andExpect(status().isOk()).andReturn();
        ExpiredTransactionsProcessedDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ExpiredTransactionsProcessedDTO.class
        );
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getProcessedTransactions());
        Mockito.verify(transactionInProgressServiceMock).findAndUpdateExpiredTransactionsStatus(any());
    }

    @Test
    void shouldReturnUErrorOnUpdateExpiredTransactionKO() throws Exception {
        Mockito.when(transactionInProgressServiceMock.findAndUpdateExpiredTransactionsStatus(eq(INITIATIVE_ID)))
                .thenThrow(new ExpirationStatusUpdateException("Error"));
        mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/update-status",
                        INITIATIVE_ID)
        ).andExpect(status().isInternalServerError());
        Mockito.verify(transactionInProgressServiceMock).findAndUpdateExpiredTransactionsStatus(any());
    }


    @Test
    void shouldReturnResentTransactionNumberOnValidRequest() throws Exception {
        Mockito.when(transactionInProgressServiceMock.sendEventForStaleExpiredTransactions(eq(INITIATIVE_ID)))
                .thenReturn(1L);
        MvcResult result = mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/resend",
                        INITIATIVE_ID)
        ).andExpect(status().isOk()).andReturn();
        ExpiredTransactionsProcessedDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ExpiredTransactionsProcessedDTO.class
        );
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getProcessedTransactions());
        Mockito.verify(transactionInProgressServiceMock).sendEventForStaleExpiredTransactions(any());
    }

    @Test
    void shouldReturnErrorOnResendExpiredTransactionKO() throws Exception {
        Mockito.when(transactionInProgressServiceMock.sendEventForStaleExpiredTransactions(eq(INITIATIVE_ID)))
                .thenThrow(new ExpirationStatusUpdateException("Error"));
        mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/resend",
                        INITIATIVE_ID)
        ).andExpect(status().isInternalServerError());
        Mockito.verify(transactionInProgressServiceMock).sendEventForStaleExpiredTransactions(any());
    }


}
