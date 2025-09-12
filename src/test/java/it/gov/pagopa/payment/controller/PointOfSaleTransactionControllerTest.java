package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.dto.mapper.PointOfSaleTransactionMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.PointOfSaleTransactionService;
import it.gov.pagopa.payment.test.fakers.PointOfSaleTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointOfSaleTransactionControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class})
class PointOfSaleTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PointOfSaleTransactionService pointOfSaleTransactionServiceMock;
    @MockBean
    private PointOfSaleTransactionMapper pointOfSaleTransactionMapper;

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String FISCAL_CODE = "FISCAL_CODE";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String POINT_OF_SALE_ID = "POINT_OF_SALE_ID";

    @Test
    void getPointOfSaleTransactionsList() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<TransactionInProgress> trxPage = new PageImpl<>(List.of(trx), pageRequest, 1
        );

        Mockito.when(pointOfSaleTransactionServiceMock.getPointOfSaleTransactions(
                        anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(trxPage);

        PointOfSaleTransactionDTO pointOfSaleTransactionDTO = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransactionDTO.setFiscalCode(FISCAL_CODE);

        Mockito.when(pointOfSaleTransactionMapper.toPointOfSaleTransactionDTO(trx, FISCAL_CODE))
                .thenReturn(pointOfSaleTransactionDTO);

        MvcResult result = mockMvc.perform(
                get("/idpay/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions",
                        INITIATIVE_ID, POINT_OF_SALE_ID)
                        .header("x-merchant-id", MERCHANT_ID)
                        .param("fiscalCode", FISCAL_CODE)
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", SyncTrxStatus.CREATED.toString())
        ).andExpect(status().isOk()).andReturn();

        PointOfSaleTransactionsListDTO actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PointOfSaleTransactionsListDTO.class
        );

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getTotalElements());
        Assertions.assertEquals(1, actual.getTotalPages());
        Assertions.assertEquals(1, actual.getContent().size());
        Assertions.assertEquals(trx.getTrxCode(), actual.getContent().get(0).getTrxCode());
        Assertions.assertEquals(FISCAL_CODE, actual.getContent().get(0).getFiscalCode());

        Mockito.verify(pointOfSaleTransactionServiceMock).getPointOfSaleTransactions(
                anyString(), anyString(), anyString(), anyString(), anyString(), any());
        Mockito.verify(pointOfSaleTransactionMapper).toPointOfSaleTransactionDTO(trx, FISCAL_CODE);
    }
}
