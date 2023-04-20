package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import it.gov.pagopa.payment.service.qrcode.QRCodePreAuthService;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {
    @Mock
    TransactionInProgressRepository transactionInProgressRepository;
    @Mock
    QRCodeCreationService qrCodeCreationService;
    @Mock
     QRCodePreAuthService qrCodePreAuthService;
    @Mock
    QRCodeAuthPaymentService qrCodeAuthPaymentService;
    @Mock
    QRCodeConfirmationService qrCodeConfirmationService;
    @Mock
    TransactionInProgress2SyncTrxStatusMapper transactionMapper;
    QRCodePaymentServiceImpl qrCodePaymentService;

    @BeforeEach
    void setUp(){
        qrCodePaymentService= new QRCodePaymentServiceImpl(qrCodeCreationService,
                qrCodePreAuthService,
                qrCodeAuthPaymentService,
                qrCodeConfirmationService,
                transactionInProgressRepository,
                transactionMapper);
    }
    @Test
    void getStatusTransaction() {
        //given
        SyncTrxStatusDTO trxStatus_1= SyncTrxStatusFaker.mockInstance(1);
        String transactionId="TRANSACTIONID1";
        String merchantId="MERCHANTID1";
        String acquirerId="ACQUIRERID1";
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        doReturn(Optional.of(transaction)).when(transactionInProgressRepository).findByIdAndMerchantIdAndAcquirerId(transactionId,merchantId,acquirerId);
        doReturn(trxStatus_1).when(transactionMapper).transactionInProgressMapper(transaction);
        //when
        SyncTrxStatusDTO trx= qrCodePaymentService.getStatusTransaction(transactionId,merchantId,acquirerId);
        //then
        Assertions.assertNotNull(trx);
        Assertions.assertEquals(trxStatus_1,trx);
        Assertions.assertInstanceOf(SyncTrxStatusDTO.class,trx);
        Mockito.verify(transactionInProgressRepository).findByIdAndMerchantIdAndAcquirerId(anyString(),anyString(),anyString());
        Mockito.verify(transactionMapper).transactionInProgressMapper(any());
    }

    @Test
    void getStatusTransactionException(){
        //given
        doReturn(Optional.empty()).when(transactionInProgressRepository)
                .findByIdAndMerchantIdAndAcquirerId("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1");
        //when
        //then
        ClientExceptionNoBody clientExceptionNoBody= assertThrows(ClientExceptionNoBody.class,
                ()-> qrCodePaymentService.getStatusTransaction("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1"));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, clientExceptionNoBody.getHttpStatus());
        Assertions.assertEquals("Transaction does not exist", clientExceptionNoBody.getMessage());
    }
}