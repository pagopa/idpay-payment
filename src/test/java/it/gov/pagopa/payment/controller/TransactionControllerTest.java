package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.configuration.JsonConfig;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.TransactionService;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionControllerImpl.class)
@Import(JsonConfig.class)
class TransactionControllerTest {

  @MockBean
  private TransactionService transactionService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String TRANSACTION_ID = "TRANSACTIONID1";

  private static final String USER_ID = "USERID1";

  @Test
  void getTransaction() throws Exception {
    SyncTrxStatusDTO expectedTrx = SyncTrxStatusFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

    when(transactionService.getTransaction(any(String.class), any(String.class)))
        .thenReturn(expectedTrx);

    MvcResult result = mockMvc.perform(
            get("/idpay/payment/transaction/{transactionId}", TRANSACTION_ID)
                .header("x-user-id", USER_ID))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    SyncTrxStatusDTO trx = objectMapper.readValue(
        result.getResponse().getContentAsString(),
            SyncTrxStatusDTO.class);

    Assertions.assertEquals(expectedTrx, trx);
  }
}
