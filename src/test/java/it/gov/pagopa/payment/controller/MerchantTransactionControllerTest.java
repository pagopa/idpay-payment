package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.MerchantTransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantTransactionControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class})
class MerchantTransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private MerchantTransactionService merchantTransactionServiceMock;
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String FISCAL_CODE = "FISCAL_CODE";
    private static final String MERCHANT_ID = "MERCHANT_ID";

    @Test
    void getMerchantTransactionsList() throws Exception {
        MerchantTransactionsListDTO dto = MerchantTransactionsListDTO.builder()
                .content(Collections.emptyList())
                .pageNo(1)
                .pageSize(1)
                .totalElements(1)
                .totalPages(1).build();

        Mockito.when(merchantTransactionServiceMock.getMerchantTransactions(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(dto);

        MvcResult result = mockMvc.perform(
                get("/idpay/merchant/portal/initiatives/{initiativeId}/transactions", INITIATIVE_ID)
                        .header("x-merchant-id", MERCHANT_ID)
                        .param("fiscalCode", FISCAL_CODE)
                        .param("page", String.valueOf(1))
                        .param("size", String.valueOf(10))
                        .param("status", SyncTrxStatus.CREATED.toString())
        ).andExpect(status().is2xxSuccessful()).andReturn();

        MerchantTransactionsListDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                MerchantTransactionsListDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(dto,resultResponse);
        Mockito.verify(merchantTransactionServiceMock).getMerchantTransactions(anyString(),anyString(), anyString(), anyString(), any());
    }
}
