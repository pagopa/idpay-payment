package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.MerchantTransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value={MerchantTransactionControllerImpl.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
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
    private static final String MERCHANT_ID = "MERCHANT_ID\n!";

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
        Mockito.verify(merchantTransactionServiceMock).getMerchantTransactions(eq("MERCHANT_ID"), eq(INITIATIVE_ID), eq(FISCAL_CODE), eq(SyncTrxStatus.CREATED.toString()), any());
    }

    @Test
    void getMerchantTransactionsProcessedList() throws Exception {
        MerchantTransactionsListDTO dto = MerchantTransactionsListDTO.builder()
                .content(Collections.emptyList())
                .pageNo(1)
                .pageSize(1)
                .totalElements(1)
                .totalPages(1).build();

        Mockito.when(merchantTransactionServiceMock.getMerchantTransactionsProcessed(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(dto);

        MvcResult result = mockMvc.perform(
                get("/idpay/merchant/portal/initiatives/{initiativeId}/transactions/processed", INITIATIVE_ID)
                        .header("x-merchant-id", MERCHANT_ID)
                        .header("x-organization-role", "ROLE\n!")
                        .param("fiscalCode", FISCAL_CODE)
                        .param("page", String.valueOf(1))
                        .param("size", String.valueOf(10))
                        .param("status", SyncTrxStatus.CREATED.toString())
                        .param("rewardBatchId", "BATCH-1")
                        .param("rewardBatchTrxStatus", "CONSULTABLE")
                        .param("pointOfSaleId", "POS-1")
                        .param("trxCode", "TRX-1")
        ).andExpect(status().is2xxSuccessful()).andReturn();

        MerchantTransactionsListDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                MerchantTransactionsListDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(dto, resultResponse);
        Mockito.verify(merchantTransactionServiceMock).getMerchantTransactionsProcessed(
                eq("MERCHANT_ID"),
                eq("ROLE"),
                eq(INITIATIVE_ID),
                eq(FISCAL_CODE),
                eq(SyncTrxStatus.CREATED.toString()),
                eq("BATCH-1"),
                eq("CONSULTABLE"),
                eq("POS-1"),
                eq("TRX-1"),
                any()
        );
    }
}
