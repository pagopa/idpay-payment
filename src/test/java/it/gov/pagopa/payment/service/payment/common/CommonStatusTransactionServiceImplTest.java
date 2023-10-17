package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapperTest;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CommonStatusTransactionServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    private CommonStatusTransactionServiceImpl commonStatusTransactionService;

    @BeforeEach
    void setUp(){
        commonStatusTransactionService = new CommonStatusTransactionServiceImpl(transactionInProgressRepositoryMock,
                new TransactionInProgress2SyncTrxStatusMapper());
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
        SyncTrxStatusDTO result= commonStatusTransactionService.getStatusTransaction(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
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
        ClientExceptionNoBody clientExceptionNoBody= assertThrows(ClientExceptionNoBody.class,
                ()-> commonStatusTransactionService.getStatusTransaction("TRANSACTIONID1","MERCHANTID1","ACQUIRERID1"));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, clientExceptionNoBody.getHttpStatus());
        Assertions.assertEquals("Transaction does not exist", clientExceptionNoBody.getMessage());
    }
}