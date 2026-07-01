package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.payment.utils.RewardConstants.TRX_CHANNEL_BARCODE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveActiveBarcodeTest {
    private static final String USER_ID = "USERID";
    private static final String INITIATIVE_ID = "INITIATIVEID";
    @Mock
    private TransactionInProgressRepository transactionInProgressRepositoryMock;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapperMock = new TransactionBarCodeInProgress2TransactionResponseMapper(5, 10);
    @Mock private TransactionRepository transactionRepository;

    private RetrieveActiveBarcode retrieveActiveBarcode;

    @BeforeEach
    void setUp() {
        retrieveActiveBarcode = new RetrieveActiveBarcodeImpl(transactionRepository,transactionInProgressRepositoryMock, transactionBarCodeInProgress2TransactionResponseMapperMock);
    }

    @Test
    void findOldestNotAuthorized_NotFoundInDB() {
        // Given
        Mockito.when(transactionInProgressRepositoryMock.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(Collections.emptyList());

        //When
        TransactionNotFoundOrExpiredException errorResult = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () -> retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID));

        //Then
        Assertions.assertNotNull(errorResult);
        Assertions.assertEquals("No active transaction found for user", errorResult.getMessage());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, errorResult.getCode());
    }

    @Test
    void findOldestNoAuthorized_FindWithAuthorizationTransaction(){
        // Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        TransactionInProgress trxAuth = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        Mockito.when(transactionInProgressRepositoryMock.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trx, trxAuth));

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void findOldestNoAuthorized_FindWithFewTransaction(){
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        TransactionInProgress trx1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx1.setTrxDate(now.minusMinutes(5L));
        TransactionInProgress trx2 = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trx2.setTrxDate(now.minusDays(5L));
        TransactionInProgress trx3 = TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.CREATED);
        trx3.setTrxDate(now);
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(anyString(),anyString(), anyString())).thenReturn(List.of(trx));
        Mockito.when(transactionInProgressRepositoryMock.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trx1, trx2, trx3));

        TransactionBarCodeResponse trxExpected = transactionBarCodeInProgress2TransactionResponseMapperMock.apply(trx2);
        trxExpected.setResidualBudgetCents(trxExpected.getVoucherAmountCents());

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxExpected, result);
    }
}