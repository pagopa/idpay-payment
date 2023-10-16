package it.gov.pagopa.payment.service.payment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapperTest;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeCancelService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeCreationService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodePreAuthService;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeUnrelateService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceImplTest {
    @Mock
    private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock
    private QRCodeCreationService qrCodeCreationServiceMock;
    @Mock
    private QRCodePreAuthService qrCodePreAuthServiceMock;
    @Mock
    private QRCodeAuthPaymentService qrCodeAuthPaymentServiceMock;
    @Mock
    private QRCodeConfirmationService qrCodeConfirmationServiceMock;
    @Mock
    private QRCodeCancelService qrCodeCancelServiceMock;
    @Mock
    private QRCodeUnrelateService qrCodeUnrelateService;

    private final TransactionInProgress2SyncTrxStatusMapper transactionMapper= new TransactionInProgress2SyncTrxStatusMapper();

    private QRCodePaymentServiceImpl qrCodePaymentService;

    @BeforeEach
    void setUp(){
        qrCodePaymentService = new QRCodePaymentServiceImpl(qrCodeCreationServiceMock,
                qrCodePreAuthServiceMock,
                qrCodeAuthPaymentServiceMock,
                qrCodeConfirmationServiceMock,
                qrCodeCancelServiceMock,
                qrCodeUnrelateService,
                transactionInProgressRepositoryMock,
                transactionMapper);
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
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
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        doReturn(Optional.of(transaction)).when(transactionInProgressRepositoryMock).findByIdAndMerchantIdAndAcquirerId(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
        //when
        SyncTrxStatusDTO result= qrCodePaymentService.getStatusTransaction(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
        //then
        Assertions.assertNotNull(result);
        TransactionInProgress2SyncTrxStatusMapperTest.mapperAssertion(transaction,result);
    }

    @Test
    void getStatusTransaction_NotFoundException(){
        //given
        doReturn(Optional.empty()).when(transactionInProgressRepositoryMock)
                .findByIdAndMerchantIdAndAcquirerId("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1");
        //when
        //then
        NotFoundException clientExceptionNoBody= assertThrows(NotFoundException.class,
                ()-> qrCodePaymentService.getStatusTransaction("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1"));
        Assertions.assertEquals("Transaction not found", clientExceptionNoBody.getCode());
        Assertions.assertEquals("Transaction does not exist", clientExceptionNoBody.getMessage());
    }
}