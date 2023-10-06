package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarCodePaymentServiceImplTest {

    @Mock
    private BarCodeAuthPaymentService barCodeAuthPaymentService;

    private BarCodePaymentService barCodePaymentService;

    @BeforeEach
    void setup(){
        barCodePaymentService = new BarCodePaymentServiceImpl(barCodeAuthPaymentService);
    }

    @Test
    void authPayment(){
        // Given
        String trxCode = "TRX_CODE";
        String merchantId = "MERCHANT_ID";
        long amountCents = 1000;
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        Mockito.when(barCodeAuthPaymentService.authPayment(trxCode, merchantId, amountCents))
                .thenReturn(authPaymentDTO);

        // When
        AuthPaymentDTO result = barCodePaymentService.authPayment(trxCode, amountCents, merchantId);

        // Then
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Mockito.verify(barCodeAuthPaymentService, Mockito.times(1)).authPayment(trxCode, merchantId, amountCents);
        Mockito.verifyNoMoreInteractions(barCodeAuthPaymentService);
    }
}
