package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.ExpiredTransactionsProcessedDTO;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.service.payment.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value={ExpiredTransactionsController.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class})
public class ExpiredTransactionsControllerTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldReturnUpdatedTransactionNumberOnValidRequest() throws Exception {
        when(transactionService.findAndUpdateExpiredTransactionsStatus(INITIATIVE_ID))
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
        verify(transactionService).findAndUpdateExpiredTransactionsStatus(any());
    }

    @Test
    void shouldReturnUErrorOnUpdateExpiredTransactionKO() throws Exception {
        when(transactionService.findAndUpdateExpiredTransactionsStatus(INITIATIVE_ID))
                .thenThrow(new ExpirationStatusUpdateException("Error"));
        mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/update-status",
                        INITIATIVE_ID)
        ).andExpect(status().isInternalServerError());
        verify(transactionService).findAndUpdateExpiredTransactionsStatus(any());
    }


    @Test
    void shouldReturnResentTransactionNumberOnValidRequest() throws Exception {
        when(transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID))
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
        verify(transactionService).sendEventForStaleExpiredTransactions(any());
    }

    @Test
    void shouldReturnErrorOnResendExpiredTransactionKO() throws Exception {
        when(transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID))
                .thenThrow(new ExpirationStatusUpdateException("Error"));
        mockMvc.perform(
                post("/idpay/transactions/expired/initiatives/{initiativeId}/resend",
                        INITIATIVE_ID)
        ).andExpect(status().isInternalServerError());
        verify(transactionService).sendEventForStaleExpiredTransactions(any());
    }


}
