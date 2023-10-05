package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.QRCodePaymentService;
import it.gov.pagopa.payment.service.payment.qrcode.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class})
class QRCodePaymentControllerImplTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QRCodePaymentService qrCodePaymentServiceMock;
    @MockBean
    private QRCodeExpirationService qrCodeExpirationServiceMock;

    @Test
    void getStatusTransaction() throws Exception {
        SyncTrxStatusDTO trx= SyncTrxStatusFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        Mockito.when(qrCodePaymentServiceMock.getStatusTransaction(trx.getId(), trx.getMerchantId(), trx.getAcquirerId())).thenReturn(trx);

        MvcResult result= mockMvc.perform(
                get("/idpay/payment/qr-code/merchant/status/{transactionId}",trx.getId())
                        .header("x-merchant-id",trx.getMerchantId())
                        .header("x-acquirer-id",trx.getAcquirerId())
        ).andExpect(status().is2xxSuccessful())
                .andReturn();

        SyncTrxStatusDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SyncTrxStatusDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(trx,resultResponse);
        Mockito.verify(qrCodePaymentServiceMock).getStatusTransaction(anyString(),anyString(),anyString());
    }

    @Test
    void getStatusTransaction_NotFoundException() throws Exception {

        Mockito.when(qrCodePaymentServiceMock.getStatusTransaction("TRANSACTIONID","MERCHANTID","ACQUIRERID"))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

        mockMvc.perform(
                        get("/idpay/payment/qr-code/merchant/status/{transactionId}","TRANSACTIONID")
                                .header("x-merchant-id","MERCHANTID")
                                .header("x-acquirer-id","ACQUIRERID")
                ).andExpect(status().isNotFound())
                .andExpect(res -> Assertions.assertTrue(res.getResolvedException() instanceof ClientExceptionNoBody))
                .andReturn();
        Mockito.verify(qrCodePaymentServiceMock).getStatusTransaction(anyString(),anyString(),anyString());
    }
}