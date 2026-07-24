package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCaptureService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationService;
import it.gov.pagopa.payment.service.payment.barcode.RetrieveActiveBarcode;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodePaymentServiceImplTest {

    @Mock
    private BarCodeCreationService barCodeCreationService;
    @Mock
    private BarCodeCaptureService barCodeCaptureService;
    @Mock
    private BarCodeAuthPaymentService barCodeAuthPaymentService;
    @Mock
    private RetrieveActiveBarcode retrieveActiveBarcode;

    @InjectMocks
    private BarCodePaymentServiceImpl barCodePaymentService;

    @Test
    void createTransaction() {
        TransactionBarCodeCreationRequest trxBRCodeCreationRequest = TransactionBarCodeCreationRequestFaker.mockInstance(1);
        String userId = "USERID";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        when(barCodeCreationService.createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.createTransaction(trxBRCodeCreationRequest, userId);

        Assertions.assertEquals(response.getId(), result.getId());
        Assertions.assertEquals(response, result);
        verify(barCodeCreationService, times(1)).createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId);
        verifyNoMoreInteractions(barCodeCreationService);
    }

    @Test
    void authPayment() {
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(1000L)
                .idTrxAcquirer("ID_TRX_ACQUIRER")
                .build();
        String trxCode = "TRX_CODE";
        String merchantId = "MERCHANT_ID";
        String pointOfSaleId = "POS_ID";
        String acquirerID = "ACQUIRER_ID";
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        when(barCodeAuthPaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID);

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        verify(barCodeAuthPaymentService, times(1)).authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID);
        verifyNoMoreInteractions(barCodeAuthPaymentService);
    }

    @Test
    void capturePayment_ok() {
        String initiativeId = "initiativeId";
        String trxCode = "trxCode";
        String merchantId = "merchantId";
        String pointOfSaleId = "pointOfSaleId";
        String acquirerId = "acquirerId";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        when(barCodeCaptureService.capturePayment(initiativeId, trxCode, merchantId, pointOfSaleId, acquirerId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.capturePayment(initiativeId, trxCode, merchantId, pointOfSaleId, acquirerId);

        Assertions.assertNotNull(result);
        verify(barCodeCaptureService).capturePayment(initiativeId, trxCode, merchantId, pointOfSaleId, acquirerId);
    }

    @Test
    void retriveVoucher_ok() {
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        when(barCodeCaptureService.retriveVoucher("initiativeId", "trxCode", "userId"))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.retriveVoucher("initiativeId", "trxCode", "userId");

        Assertions.assertEquals(response, result);
        verify(barCodeCaptureService).retriveVoucher("initiativeId", "trxCode", "userId");
        verifyNoMoreInteractions(barCodeCaptureService);
    }

    @Test
    void previewPayment_ok() {
        PreviewPaymentResultDTO previewPaymentResultDTO = PreviewPaymentResultDTO.builder()
                .trxCode("trxCode")
                .trxDate(OffsetDateTime.now())
                .status(SyncTrxStatus.AUTHORIZED)
                .originalAmountCents(500L)
                .rewardCents(100L)
                .residualAmountCents(400L)
                .userId("userId")
                .additionalProperties(Map.of("productGtin", "gtin"))
                .extendedAuthorization(false)
                .build();
        Map<String, String> additionalProperties = Map.of("productGtin", "gtin");

        when(barCodeAuthPaymentService.previewPayment(any(), any(), any(), any()))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResultDTO result = barCodePaymentService.previewPayment("initiativeId", "trxCode", additionalProperties, 500L);

        Assertions.assertNotNull(result);
        verify(barCodeAuthPaymentService).previewPayment("initiativeId", "trxCode", additionalProperties, 500L);
    }

    @Test
    void findOldestNotAuthorized_ok() {
        String userId = "USER_ID";
        String initiativeId = "INITIATIVE_ID";

        TransactionBarCodeResponse trx = TransactionBarCodeResponseFaker.mockInstance(1);
        when(retrieveActiveBarcode.findOldestNotAuthorized(userId, initiativeId)).thenReturn(trx);

        TransactionBarCodeResponse result = barCodePaymentService.findOldestNotAuthorized(userId, initiativeId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx, result);
    }

    @Test
    void createExtendedTransaction_ok() {
        TransactionBarCodeCreationRequest trxBRCodeCreationRequest = TransactionBarCodeCreationRequestFaker.mockInstance(1);
        String userId = "USERID";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        when(barCodeCreationService.createExtendedTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.createExtendedTransaction(trxBRCodeCreationRequest, userId);

        Assertions.assertEquals(response.getId(), result.getId());
        Assertions.assertEquals(response, result);
        verify(barCodeCreationService, times(1)).createExtendedTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId);
        verifyNoMoreInteractions(barCodeCreationService);
    }
}
