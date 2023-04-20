package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusTest;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import it.gov.pagopa.payment.service.qrcode.QRCodePreAuthService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {
    @Mock
    TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock
    QRCodeCreationService qrCodeCreationServiceMock;
    @Mock
     QRCodePreAuthService qrCodePreAuthServiceMock;
    @Mock
    QRCodeAuthPaymentService qrCodeAuthPaymentServiceMock;
    @Mock
    QRCodeConfirmationService qrCodeConfirmationServiceMock;
    @Spy
    private final TransactionInProgress2SyncTrxStatusMapper transactionMapper= new TransactionInProgress2SyncTrxStatusMapper();

    QRCodePaymentServiceImpl qrCodePaymentServiceMock;

    @BeforeEach
    void setUp(){
        qrCodePaymentServiceMock = new QRCodePaymentServiceImpl(qrCodeCreationServiceMock,
                qrCodePreAuthServiceMock,
                qrCodeAuthPaymentServiceMock,
                qrCodeConfirmationServiceMock,
                transactionInProgressRepositoryMock,
                transactionMapper);
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(
                transactionInProgressRepositoryMock,
                qrCodeCreationServiceMock,
                qrCodePreAuthServiceMock,
                qrCodeAuthPaymentServiceMock,
                qrCodeConfirmationServiceMock);
    }

    @Test
    void getStatusTransaction() {
        //given
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
                .authDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        doReturn(Optional.of(transaction)).when(transactionInProgressRepositoryMock).findByIdAndMerchantIdAndAcquirerId(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
        //when
        SyncTrxStatusDTO result= qrCodePaymentServiceMock.getStatusTransaction(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
        //then
        Assertions.assertNotNull(result);
        TransactionInProgress2SyncTrxStatusTest.mapperAssertion(transaction,result);
    }

    @Test
    void getStatusTransaction_NotFoundException(){
        //given
        doReturn(Optional.empty()).when(transactionInProgressRepositoryMock)
                .findByIdAndMerchantIdAndAcquirerId("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1");
        //when
        //then
        ClientExceptionNoBody clientExceptionNoBody= assertThrows(ClientExceptionNoBody.class,
                ()-> qrCodePaymentServiceMock.getStatusTransaction("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1"));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, clientExceptionNoBody.getHttpStatus());
        Assertions.assertEquals("Transaction does not exist", clientExceptionNoBody.getMessage());
        Mockito.verify(transactionMapper, never()).transactionInProgressMapper(any());
    }
}