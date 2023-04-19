package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.configuration.JsonConfig;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import(JsonConfig.class)
class QRCodePaymentControllerImplTest {
@MockBean
private QRCodePaymentService qrCodePaymentService;
@Autowired
private MockMvc mockMvc;
@Autowired
private ObjectMapper objectMapper;

    @Test
    void getStatusTransaction() throws Exception {
        SyncTrxStatusDTO trxStatus_2= SyncTrxStatusFaker.mockInstance(2);
        String transactionId="TRANSACTIONID2";
        String merchantId="MERCHANTID2";
        String acquirerId="ACQUIRERID2";

        Mockito.when(qrCodePaymentService.getStatusTransaction(transactionId,merchantId ,acquirerId)).thenReturn(trxStatus_2);

        MvcResult result= mockMvc.perform(
                get("/idpay/payment/qr-code/status/{transactionId}",transactionId)
                        .header("x-merchant-id",merchantId)
                        .header("x-acquirer-id",acquirerId)
        ).andExpect(status().is2xxSuccessful())
                .andReturn();

        SyncTrxStatusDTO trx = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SyncTrxStatusDTO.class);


        Assertions.assertNotNull(trx);
        Assertions.assertEquals(trxStatus_2,trx);
        Mockito.verify(qrCodePaymentService).getStatusTransaction(anyString(),anyString(),anyString());

    }

    @Test
    void getStatusTransactionException() throws Exception {

        Mockito.when(qrCodePaymentService.getStatusTransaction("TRANSACTIONID","MERCHANTID","ACQUIRERID"))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

        MvcResult result= mockMvc.perform(
                        get("/idpay/payment/qr-code/status/{transactionId}","TRANSACTIONID")
                                .header("x-merchant-id","MERCHANTID")
                                .header("x-acquirer-id","ACQUIRERID")
                ).andExpect(status().isNotFound())
                .andExpect(res -> Assertions.assertTrue(res.getResolvedException() instanceof ClientExceptionNoBody))
                .andReturn();
        Mockito.verify(qrCodePaymentService).getStatusTransaction(anyString(),anyString(),anyString());
    }

}