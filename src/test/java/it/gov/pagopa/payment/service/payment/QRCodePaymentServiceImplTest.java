package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.common.CommonStatusTransactionServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.*;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {
    @Mock
    private QRCodeCreationService qrCodeCreationServiceMock;
    @Mock
    private QRCodePreAuthService qrCodePreAuthServiceMock;
    @Mock
    private QRCodeAuthPaymentService qrCodeAuthPaymentServiceMock;
    @Mock
    private QRCodeUnrelateService qrCodeUnrelateService;
    @Mock
    private CommonStatusTransactionServiceImpl commonStatusTransactionServiceMock;

    @BeforeEach
    void setUp(){
       QRCodePaymentServiceImpl qrCodePaymentService = new QRCodePaymentServiceImpl(qrCodeCreationServiceMock,
                qrCodePreAuthServiceMock,
                qrCodeAuthPaymentServiceMock,
                qrCodeUnrelateService);
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        Mockito.verifyNoMoreInteractions(
                qrCodeCreationServiceMock,
                qrCodePreAuthServiceMock,
                qrCodeAuthPaymentServiceMock);
    }

    @Test
    void getStatusTransaction() {
        //given
        SyncTrxStatusDTO trxStatus = SyncTrxStatusFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        when(commonStatusTransactionServiceMock.getStatusTransaction(trxStatus.getId()))
                .thenReturn(trxStatus);
        //when
        SyncTrxStatusDTO result= commonStatusTransactionServiceMock.getStatusTransaction(trxStatus.getId());
        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxStatus, result);
    }
}