package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodePreAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeUnrelateService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {

    @Mock
    private QRCodePreAuthServiceImpl qrCodePreAuthService;
    @Mock
    private QRCodeAuthPaymentService qrCodeAuthPaymentService;

    @Mock
    private QRCodeUnrelateService qrCodeUnrelateService;

    @InjectMocks
    private QRCodePaymentServiceImpl qrCodePaymentService;

    @Test
    void relateUser(){
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        when(qrCodePreAuthService.relateUser(trx.getTrxCode(), trx.getUserId()))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = qrCodePaymentService.relateUser(trx.getTrxCode(), trx.getUserId());

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO, result);
        verify(qrCodePreAuthService, times(1)).relateUser(trx.getTrxCode(), trx.getUserId());
        verifyNoMoreInteractions(qrCodePreAuthService);
    }
    @Test
    void authPayment(){

        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);

        when(qrCodeAuthPaymentService.authPayment(trx.getUserId(),trx.getTrxCode()))
                .thenReturn(authPaymentDTO);

        AuthPaymentDTO result = qrCodePaymentService.authPayment(trx.getUserId(),trx.getTrxCode());

        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        Assertions.assertEquals(authPaymentDTO.getId(), result.getId());
        verify(qrCodeAuthPaymentService, times(1)).authPayment(trx.getUserId(),trx.getTrxCode());
        verifyNoMoreInteractions(qrCodeAuthPaymentService);
    }

    @Test
    void unrelateUser(){

        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        doNothing().when(qrCodeUnrelateService).unrelateTransaction(trx.getTrxCode(),trx.getUserId());

        qrCodePaymentService.unrelateUser(trx.getTrxCode(),trx.getUserId());

        verify(qrCodeUnrelateService, times(1)).unrelateTransaction(trx.getTrxCode(),trx.getUserId());
        verifyNoMoreInteractions(qrCodeUnrelateService);
    }
}
