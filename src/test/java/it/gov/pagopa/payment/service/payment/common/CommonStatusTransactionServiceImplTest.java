package it.gov.pagopa.payment.service.payment.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapperTest;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonStatusTransactionServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock = new TransactionInProgress2TransactionResponseMapper(5, "qrcodeImgBaseUrl", "qrcodeImgBaseUrl");

    private CommonStatusTransactionServiceImpl commonStatusTransactionService;

    @BeforeEach
    void setUp(){
        commonStatusTransactionService = new CommonStatusTransactionServiceImpl(transactionInProgressRepositoryMock,
                new TransactionInProgress2SyncTrxStatusMapper(transactionInProgress2TransactionResponseMapperMock));
    }

    @Test
    void getStatusTransaction() {
        //given
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        doReturn(Optional.of(transaction)).when(transactionInProgressRepositoryMock).findById(transaction.getId());
        //when
        SyncTrxStatusDTO result= commonStatusTransactionService.getStatusTransaction(transaction.getId(), transaction.getMerchantId());
        //then
        Assertions.assertNotNull(result);
        TransactionInProgress2SyncTrxStatusMapperTest.mapperAssertion(transaction,result);
    }

    @Test
    void getStatusTransactionQRCode() {
        //given
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        transaction.setChannel("QRCODE");
        doReturn(Optional.of(transaction)).when(transactionInProgressRepositoryMock).findById(transaction.getId());
        //when
        SyncTrxStatusDTO result= commonStatusTransactionService.getStatusTransaction(transaction.getId(), transaction.getMerchantId());
        //then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getQrcodePngUrl());
        Assertions.assertNotNull(result.getQrcodeTxtUrl());
        TransactionInProgress2SyncTrxStatusMapperTest.mapperAssertion(transaction,result);
    }

    @Test
    void getStatusTransactionNotAuthorized() {
        //given
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        String trxId = transaction.getId();
        doReturn(Optional.of(transaction)).when(transactionInProgressRepositoryMock).findById(trxId);

        //when
        TransactionNotFoundOrExpiredException clientExceptionNoBody= assertThrows(TransactionNotFoundOrExpiredException.class,
                ()-> commonStatusTransactionService.getStatusTransaction(trxId, "DUMMYMERCHANTID"));

        //then
        Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", clientExceptionNoBody.getCode());
        Assertions.assertEquals("Cannot find transaction with transactionId [" + trxId + "]", clientExceptionNoBody.getMessage());
    }

    @Test
    void getStatusTransaction_NotFoundException(){
        //given
        doReturn(Optional.empty()).when(transactionInProgressRepositoryMock)
                .findById("TRANSACTIONID1");
        //when
        //then
        TransactionNotFoundOrExpiredException clientExceptionNoBody= assertThrows(TransactionNotFoundOrExpiredException.class,
                ()-> commonStatusTransactionService.getStatusTransaction("TRANSACTIONID1", "MERCHANTID"));
        Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", clientExceptionNoBody.getCode());
        Assertions.assertEquals("Cannot find transaction with transactionId [TRANSACTIONID1]", clientExceptionNoBody.getMessage());
    }
}