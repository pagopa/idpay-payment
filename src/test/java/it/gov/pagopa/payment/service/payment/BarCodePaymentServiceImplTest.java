package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationService;
import it.gov.pagopa.payment.test.fakers.*;
import it.gov.pagopa.payment.utils.RewardConstants;
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
    private BarCodeAuthPaymentService barCodeAuthPaymentService;

    private BarCodePaymentService barCodePaymentService;


    @BeforeEach
    void setup(){
        barCodePaymentService = new BarCodePaymentServiceImpl(barCodeCreationService, barCodeAuthPaymentService);
    }

    @Test
    void createTransaction(){
        // Given
        TransactionBarCodeCreationRequest trxBRCodeCreationRequest = TransactionBarCodeCreationRequestFaker.mockInstance(1);
        String userId = "USERID";
        TransactionBarCodeResponse response = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodeCreationService.createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId))
                .thenReturn(response);

        TransactionBarCodeResponse result = barCodePaymentService.createTransaction(trxBRCodeCreationRequest, userId);

        // Then
        Assertions.assertEquals(response.getId(), result.getId());
        Assertions.assertEquals(response, result);
        Mockito.verify(barCodeCreationService, Mockito.times(1)).createTransaction(trxBRCodeCreationRequest, RewardConstants.TRX_CHANNEL_BARCODE, userId);
        Mockito.verifyNoMoreInteractions(barCodeCreationService);
    }
    @Test
    void authPayment(){
        // Given
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(1000L)
                .idTrxAcquirer("ID_TRX_ACQUIRER")
                .build();
        String trxCode = "TRX_CODE";
        String merchantId = "MERCHANT_ID";
        String acquirerID = "ACQUIRER_ID";
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        Mockito.when(barCodeAuthPaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, acquirerID))
                .thenReturn(authPaymentDTO);

        // When
        AuthPaymentDTO result = barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, acquirerID);

        // Then
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Mockito.verify(barCodeAuthPaymentService, Mockito.times(1)).authPayment(trxCode, authBarCodePaymentDTO, merchantId, acquirerID);
        Mockito.verifyNoMoreInteractions(barCodeAuthPaymentService);
    }


    @Test
    void previewPayment_ok(){
        PreviewPaymentDTO previewPaymentDTO = PreviewPaymentDTOFaker.mockInstance();

        Mockito.when(barCodeAuthPaymentService.previewPayment(any()))
                .thenReturn(previewPaymentDTO);

        // When
        PreviewPaymentDTO result = barCodePaymentService.previewPayment("trxCode");

        // Then
        Assertions.assertNotNull(result);
    }
}
