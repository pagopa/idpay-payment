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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;

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

    private BarCodePaymentService barCodePaymentService;

    @BeforeEach
    void setup() {
        barCodePaymentService = new BarCodePaymentServiceImpl(barCodeCreationService, barCodeCaptureService, barCodeAuthPaymentService, retrieveActiveBarcode);
    }

    @Test
    void createTransaction() {
        TransactionBarCodeCreationRequest trxBRCodeCreationRequest = TransactionBarCodeCreationRequestFaker.mockInstance(1);
        String userId = "USERID";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodeCreationService.createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.createTransaction(trxBRCodeCreationRequest, userId);

        Assertions.assertEquals(response.getId(), result.getId());
        Assertions.assertEquals(response, result);
        Mockito.verify(barCodeCreationService, Mockito.times(1)).createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId);
        Mockito.verifyNoMoreInteractions(barCodeCreationService);
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

        Mockito.when(barCodeAuthPaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID);

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Mockito.verify(barCodeAuthPaymentService, Mockito.times(1)).authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerID);
        Mockito.verifyNoMoreInteractions(barCodeAuthPaymentService);
    }

    @Test
    void capturePayment_ok() {
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodeCaptureService.capturePayment(any()))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.capturePayment("trxCode");

        Assertions.assertNotNull(result);
    }

    @Test
    void retriveVoucher_ok() {
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodeCaptureService.retriveVoucher("initiativeId", "trxCode", "userId"))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.retriveVoucher("initiativeId", "trxCode", "userId");

        Assertions.assertEquals(response, result);
        Mockito.verify(barCodeCaptureService).retriveVoucher("initiativeId", "trxCode", "userId");
        Mockito.verifyNoMoreInteractions(barCodeCaptureService);
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

        Mockito.when(barCodeAuthPaymentService.previewPayment(any(), any(), any()))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResultDTO result = barCodePaymentService.previewPayment("trxCode", additionalProperties, 500L);

        Assertions.assertNotNull(result);
        Mockito.verify(barCodeAuthPaymentService).previewPayment("trxCode", additionalProperties, 500L);
    }

    @Test
    void findOldestNotAuthorized_ok() {
        String userId = "USER_ID";
        String initiativeId = "INITIATIVE_ID";

        TransactionBarCodeResponse trx = TransactionBarCodeResponseFaker.mockInstance(1);
        Mockito.when(retrieveActiveBarcode.findOldestNotAuthorized(userId, initiativeId)).thenReturn(trx);

        TransactionBarCodeResponse result = barCodePaymentService.findOldestNotAuthorized(userId, initiativeId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx, result);
    }

    @Test
    void createExtendedTransaction_ok() {
        TransactionBarCodeCreationRequest trxBRCodeCreationRequest = TransactionBarCodeCreationRequestFaker.mockInstance(1);
        String userId = "USERID";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodeCreationService.createExtendedTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.createExtendedTransaction(trxBRCodeCreationRequest, userId);

        Assertions.assertEquals(response.getId(), result.getId());
        Assertions.assertEquals(response, result);
        Mockito.verify(barCodeCreationService, Mockito.times(1)).createExtendedTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId);
        Mockito.verifyNoMoreInteractions(barCodeCreationService);
    }
}
