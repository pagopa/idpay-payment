package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionControllerImpl.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    private static final String TRANSACTION_ID = "TRANSACTIONID1";

    private static final String USER_ID = "USERID1";

    @Test
    void getTransaction() throws Exception {
        when(transactionService.getTransaction(any(String.class), any(String.class)))
                .thenReturn(new TransactionInProgress());

        mockMvc.perform(
                get("/idpay/payment/transaction/{transactionId}", TRANSACTION_ID)
                        .header("x-user-id", USER_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }
}
