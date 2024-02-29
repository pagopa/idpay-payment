package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodePreAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeUnrelateService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {

    @Mock
    private QRCodePreAuthServiceImpl qrCodePreAuthService;
    @Mock
    private QRCodeAuthPaymentService qrCodeAuthPaymentService;

    @Mock
    private QRCodeUnrelateService qrCodeUnrelateService;

    private QRCodePaymentServiceImpl qrCodePaymentService;


    @BeforeEach
    void setup(){
        qrCodePaymentService = new QRCodePaymentServiceImpl(qrCodePreAuthService, qrCodeAuthPaymentService,qrCodeUnrelateService);
    }

    @Test
    void relateUser(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        Mockito.when(qrCodePreAuthService.relateUser(trx.getTrxCode(), trx.getUserId()))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = qrCodePaymentService.relateUser(trx.getTrxCode(), trx.getUserId());

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO, result);
        Mockito.verify(qrCodePreAuthService, Mockito.times(1)).relateUser(trx.getTrxCode(), trx.getUserId());
        Mockito.verifyNoMoreInteractions(qrCodePreAuthService);
    }
    @Test
    void authPayment(){

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);

        Mockito.when(qrCodeAuthPaymentService.authPayment(trx.getUserId(),trx.getTrxCode()))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = qrCodePaymentService.authPayment(trx.getUserId(),trx.getTrxCode());

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Mockito.verify(qrCodeAuthPaymentService, Mockito.times(1)).authPayment(trx.getUserId(),trx.getTrxCode());
        Mockito.verifyNoMoreInteractions(qrCodeAuthPaymentService);
    }

    @Test
    void unrelateUser(){

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        doNothing().when(qrCodeUnrelateService).unrelateTransaction(trx.getTrxCode(),trx.getUserId());

        qrCodePaymentService.unrelateUser(trx.getTrxCode(),trx.getUserId());

        Mockito.verify(qrCodeUnrelateService, Mockito.times(1)).unrelateTransaction(trx.getTrxCode(),trx.getUserId());
        Mockito.verifyNoMoreInteractions(qrCodeUnrelateService);
    }
}
