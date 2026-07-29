package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.configuration.ServiceExceptionConfig;
import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.dto.mapper.PointOfSaleTransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PosNotFoundException;
import it.gov.pagopa.payment.service.PointOfSaleTransactionService;
import it.gov.pagopa.payment.test.fakers.PointOfSaleTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value={PointOfSaleTransactionControllerImpl.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class, ServiceExceptionConfig.class})
class PointOfSaleTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private PointOfSaleTransactionService pointOfSaleTransactionServiceMock;
    @MockitoBean
    private PointOfSaleTransactionMapper pointOfSaleTransactionMapper;

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String FISCAL_CODE = "FISCAL_CODE";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String POINT_OF_SALE_ID = "POINT_OF_SALE_ID";
    private static final String PRODUCT_GTIN = "PRODUCT_GTIN";
    private static final String TRX_CODE = "TRX_CODE";

    @Test
    void getPointOfSaleTransactionsList() throws Exception {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transaction> trxPage = new PageImpl<>(List.of(trx), pageRequest, 1);

        when(pointOfSaleTransactionServiceMock.getPointOfSaleTransactions(
                any(), any()))
                .thenReturn(trxPage);

        PointOfSaleTransactionDTO pointOfSaleTransactionDTO = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransactionDTO.setFiscalCode(FISCAL_CODE);

        when(pointOfSaleTransactionMapper.toPointOfSaleTransactionDTO(trx, FISCAL_CODE))
                .thenReturn(pointOfSaleTransactionDTO);

        MvcResult result = mockMvc.perform(
                get("/idpay/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions",
                        INITIATIVE_ID, POINT_OF_SALE_ID)
                        .header("x-merchant-id", MERCHANT_ID)
                        .header("x-point-of-sale-id", POINT_OF_SALE_ID)
                        .param("fiscalCode", FISCAL_CODE)
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", SyncTrxStatus.AUTHORIZED.toString())
                        .param("productGtin", PRODUCT_GTIN)
                        .param("trxCode", TRX_CODE)
        ).andExpect(status().isOk()).andReturn();

        PointOfSaleTransactionsListDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PointOfSaleTransactionsListDTO.class
        );

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getTotalElements());
        Assertions.assertEquals(1, actual.getTotalPages());
        Assertions.assertEquals(1, actual.getContent().size());
        Assertions.assertEquals(trx.getTrxCode(), actual.getContent().getFirst().getTrxCode());
        Assertions.assertEquals(FISCAL_CODE, actual.getContent().getFirst().getFiscalCode());

        ArgumentCaptor<TrxFiltersDTO> filtersCaptor = ArgumentCaptor.forClass(TrxFiltersDTO.class);
        verify(pointOfSaleTransactionServiceMock).getPointOfSaleTransactions(
                filtersCaptor.capture(), any());
        Assertions.assertEquals(MERCHANT_ID, filtersCaptor.getValue().getMerchantId());
        Assertions.assertEquals(INITIATIVE_ID, filtersCaptor.getValue().getInitiativeId());
        Assertions.assertEquals(POINT_OF_SALE_ID, filtersCaptor.getValue().getPointOfSaleId());
        Assertions.assertEquals(FISCAL_CODE, filtersCaptor.getValue().getFiscalCode());
        Assertions.assertEquals(PRODUCT_GTIN, filtersCaptor.getValue().getProductGtin());
        Assertions.assertEquals(TRX_CODE, filtersCaptor.getValue().getTrxCode());
        verify(pointOfSaleTransactionMapper).toPointOfSaleTransactionDTO(trx, FISCAL_CODE);
    }

    @Test
    void getPointOfSaleTransactionsList_unauthorizedPointOfSale_shouldReturn403() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/idpay/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions",
                                INITIATIVE_ID, POINT_OF_SALE_ID)
                                .header("x-merchant-id", MERCHANT_ID)
                                .header("x-point-of-sale-id", "DIFFERENT_POS_ID")
                                .param("fiscalCode", FISCAL_CODE)
                                .param("page", "1")
                                .param("size", "10")
                                .param("status", SyncTrxStatus.AUTHORIZED.toString())
                                .param("productGtin", PRODUCT_GTIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertTrue(content.contains("Point of sale mismatch"));
    }

    @Test
    void getPointOfSaleTransactionsList_withoutPointOfSaleHeader() throws Exception {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transaction> trxPage = new PageImpl<>(List.of(trx), pageRequest, 1);

        when(pointOfSaleTransactionServiceMock.getPointOfSaleTransactions(
                any(), any()))
                .thenReturn(trxPage);

        PointOfSaleTransactionDTO dto = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        dto.setFiscalCode(FISCAL_CODE);

        when(pointOfSaleTransactionMapper.toPointOfSaleTransactionDTO(trx, FISCAL_CODE))
                .thenReturn(dto);

        MvcResult result = mockMvc.perform(
                get("/idpay/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions",
                        INITIATIVE_ID, POINT_OF_SALE_ID)
                        .header("x-merchant-id", MERCHANT_ID)
                        .param("fiscalCode", FISCAL_CODE)
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", SyncTrxStatus.AUTHORIZED.toString())
                        .param("productGtin", PRODUCT_GTIN)
                        .param("trxCode", TRX_CODE)
        ).andExpect(status().isOk()).andReturn();

        PointOfSaleTransactionsListDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PointOfSaleTransactionsListDTO.class
        );

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getTotalElements());

        ArgumentCaptor<TrxFiltersDTO> filtersCaptor = ArgumentCaptor.forClass(TrxFiltersDTO.class);
        verify(pointOfSaleTransactionServiceMock).getPointOfSaleTransactions(filtersCaptor.capture(), any());
        Assertions.assertEquals(POINT_OF_SALE_ID, filtersCaptor.getValue().getPointOfSaleId());
    }

    @Test
    void getPointOfSaleTransactionsProcessed_shouldSanitizeFilters() throws Exception {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transaction> trxPage = new PageImpl<>(List.of(trx), pageRequest, 1);

        when(pointOfSaleTransactionServiceMock.getPointOfSaleTransactions(any(), any()))
                .thenReturn(trxPage);

        PointOfSaleTransactionDTO dto = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        dto.setFiscalCode("FISCALCODE1");
        when(pointOfSaleTransactionMapper.toPointOfSaleTransactionDTO(trx, "FISCALCODE1"))
                .thenReturn(dto);

        mockMvc.perform(
                get("/idpay/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions/processed",
                        "INIT!@#", "POS\n1")
                        .header("x-merchant-id", "MERCHANT\r\n1")
                        .header("x-point-of-sale-id", "POS\n1")
                        .param("fiscalCode", "FISCAL@CODE1")
                        .param("status", "AUTH$ORIZED")
                        .param("productGtin", "12345-@@")
                        .param("trxCode", "TRX*CODE")
        ).andExpect(status().isOk());

        ArgumentCaptor<TrxFiltersDTO> filtersCaptor = ArgumentCaptor.forClass(TrxFiltersDTO.class);
        verify(pointOfSaleTransactionServiceMock).getPointOfSaleTransactions(filtersCaptor.capture(), any());

        Assertions.assertEquals("MERCHANT1", filtersCaptor.getValue().getMerchantId());
        Assertions.assertEquals("INIT", filtersCaptor.getValue().getInitiativeId());
        Assertions.assertEquals("POS1", filtersCaptor.getValue().getPointOfSaleId());
        Assertions.assertEquals("FISCALCODE1", filtersCaptor.getValue().getFiscalCode());
        Assertions.assertEquals(List.of("AUTHORIZED"), filtersCaptor.getValue().getStatuses());
        Assertions.assertEquals("12345-", filtersCaptor.getValue().getProductGtin());
        Assertions.assertEquals("TRXCODE", filtersCaptor.getValue().getTrxCode());
    }


    @Test
    void downloadInvoiceFile_shouldSanitizeParametersAndDelegate() throws Exception {
        DownloadInvoiceResponseDTO response = DownloadInvoiceResponseDTO.builder()
                .invoiceUrl("https://signed-url")
                .build();

        when(pointOfSaleTransactionServiceMock.downloadTransactionInvoice(
                "MERCHANT1",
                "POS1",
                "TRX1"))
                .thenReturn(response);

        MvcResult result = mockMvc.perform(
                get("/idpay/{pointOfSaleId}/transactions/{transactionId}/download", "POS!@#1", "TRX*1")
                        .header("x-merchant-id", "MERCHANT\r\n1")
                        .header("x-point-of-sale-id", "POS!@#1")
        ).andExpect(status().isOk()).andReturn();

        DownloadInvoiceResponseDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DownloadInvoiceResponseDTO.class
        );

        Assertions.assertNotNull(actual);
        Assertions.assertEquals("https://signed-url", actual.getInvoiceUrl());
        verify(pointOfSaleTransactionServiceMock)
                .downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1");
    }

    @Test
    void downloadInvoiceFile_unauthorizedPointOfSale_shouldReturn403() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/idpay/{pointOfSaleId}/transactions/{transactionId}/download", POINT_OF_SALE_ID, TRX_CODE)
                        .header("x-merchant-id", MERCHANT_ID)
                        .header("x-point-of-sale-id", "DIFFERENT_POS_ID")
        ).andExpect(status().isForbidden()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Point of sale mismatch"));
    }

    @Test
    void constructor_withMessage_shouldInitializeCorrectly() {
        String message = "POS not found";

        PosNotFoundException ex = new PosNotFoundException(message);

        Assertions.assertNotNull(ex);
        Assertions.assertEquals("POS not found", ex.getMessage());
        Assertions.assertEquals("PAYMENT_POINT_OF_SALE_NOT_FOUND", ex.getCode());
        Assertions.assertNull(ex.getCause());
    }
}