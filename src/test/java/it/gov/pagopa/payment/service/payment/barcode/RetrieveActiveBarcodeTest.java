package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.InitiativeTrxConditions;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TrxCountDTO;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.payment.utils.RewardConstants.TRX_CHANNEL_BARCODE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveActiveBarcodeTest {
    private static final String USER_ID = "USERID";
    private static final String INITIATIVE_ID = "INITIATIVEID";
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RewardRuleRepository rewardRuleRepository;
    private final TransactionMapper transactionMapper = new TransactionMapper(5, 10, "url", "url");

    private RetrieveActiveBarcode retrieveActiveBarcode;

    @BeforeEach
    void setUp() {
        retrieveActiveBarcode = new RetrieveActiveBarcodeImpl(transactionRepository,transactionMapper, rewardRuleRepository);
    }

    private void mockInitiativeConfig(boolean toIncluded, String initiativeId) {
        InitiativeTrxConditions trxRule = InitiativeTrxConditions.builder()
                .trxCount(TrxCountDTO.builder().from(1L).fromIncluded(true).to(2L).toIncluded(toIncluded).build())
                .build();
        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId(initiativeId)
                .trxRule(trxRule).build();
        RewardRule rewardRule = RewardRule.builder().initiativeConfig(initiativeConfig).build();

        when(rewardRuleRepository.findById(initiativeId)).thenReturn(Optional.of(rewardRule));
    }

    @Test
    void findOldestNotAuthorized_NotFoundInDB() {
        // Given
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(Collections.emptyList());
        mockInitiativeConfig(false, INITIATIVE_ID);

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
        mockInitiativeConfig(false, INITIATIVE_ID);
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        Transaction transactionAuth = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        Transaction trxAuth = TransactionFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trx, trxAuth));
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(transaction, transactionAuth));

        //When
        TransactionAlreadyAuthorizedException errorResult = Assertions.assertThrows(TransactionAlreadyAuthorizedException.class, () -> retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID));

        //Then
        Assertions.assertNotNull(errorResult);
        Assertions.assertEquals("The maximum number of transaction authorizations (%d) has been reached".formatted(1), errorResult.getMessage());
    }

    @Test
    void findOldestNoAuthorized_FindWithFewTransaction(){
        // Given
        mockInitiativeConfig(false, INITIATIVE_ID);
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Rome"));
        Transaction trx1 = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx1.setTrxDate(now.minusMinutes(5L));
        Transaction trx2 = TransactionFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trx2.setTrxDate(now.minusDays(5L));
        Transaction trx3 = TransactionFaker.mockInstance(3, SyncTrxStatus.CREATED);
        trx3.setTrxDate(now);
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(anyString(),anyString(), anyString())).thenReturn(List.of(trx));
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trx1, trx2, trx3));

        TransactionBarCodeResponse trxExpected = transactionMapper.transactionToTransactionBarCodeResponse(trx2);
        trxExpected.setResidualBudgetCents(trxExpected.getVoucherAmountCents());

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxExpected, result);
    }

    @Test
    void findOldestNoAuthorized_FindWithCancelledTransaction(){
        // Given
        mockInitiativeConfig(false, INITIATIVE_ID);
        OffsetDateTime startDate = OffsetDateTime.now(ZoneId.of("Europe/Rome")).truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime trxEndDate = startDate.plusDays(10).truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1).truncatedTo(ChronoUnit.MILLIS);

        Transaction trxCancelled1 = TransactionFaker.mockInstance(1, SyncTrxStatus.CANCELLED);
        trxCancelled1.setTrxDate(startDate);
        trxCancelled1.setTrxEndDate(trxEndDate);
        Transaction trxCancelled2 = TransactionFaker.mockInstance(1, SyncTrxStatus.CANCELLED);
        trxCancelled2.setTrxDate(startDate);
        trxCancelled2.setTrxEndDate(trxEndDate);
        Transaction trxCreated = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trxCreated.setTrxDate(startDate);
        trxCreated.setTrxEndDate(trxEndDate);

        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trxCancelled1, trxCancelled2, trxCreated));

        TransactionBarCodeResponse trxExpected = transactionMapper.transactionToTransactionBarCodeResponse(trxCreated);
        trxExpected.setResidualBudgetCents(trxExpected.getVoucherAmountCents());

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxExpected, result);
    }

    @Test
    void findOldestNoAuthorized_FindWithAuthTrxUnderLimit(){
        // Given
        mockInitiativeConfig(true, INITIATIVE_ID);

        Transaction trxAuth = TransactionFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        Transaction trxCreated = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);


        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trxAuth, trxCreated));

        TransactionBarCodeResponse trxExpected = transactionMapper.transactionToTransactionBarCodeResponse(trxCreated);
        trxExpected.setResidualBudgetCents(trxExpected.getVoucherAmountCents());

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxExpected, result);
    }

    @Test
    void findOldestNoAuthorized_FindWithoutAuthTrxLimits(){
        // Given
        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId(INITIATIVE_ID).build();
        RewardRule rewardRule = RewardRule.builder().initiativeConfig(initiativeConfig).build();
        when(rewardRuleRepository.findById(INITIATIVE_ID)).thenReturn(Optional.of(rewardRule));


        Transaction trxAuth1 = TransactionFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        Transaction trxAuth2 = TransactionFaker.mockInstance(3, SyncTrxStatus.AUTHORIZED);
        Transaction trxAuth3 = TransactionFaker.mockInstance(4, SyncTrxStatus.AUTHORIZED);
        Transaction trxAuth4 = TransactionFaker.mockInstance(5, SyncTrxStatus.AUTHORIZED);
        Transaction trxCreated = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);


        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trxAuth1, trxAuth2, trxAuth3, trxAuth4, trxCreated));

        TransactionBarCodeResponse trxExpected = transactionMapper.transactionToTransactionBarCodeResponse(trxCreated);
        trxExpected.setResidualBudgetCents(trxExpected.getVoucherAmountCents());

        //When
        TransactionBarCodeResponse result = retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxExpected, result);
    }

    @Test
    void findOldestNoAuthorized_notFoundTransaction(){
        // Given
        mockInitiativeConfig(false, INITIATIVE_ID);
        Transaction trxCancelled = TransactionFaker.mockInstance(1, SyncTrxStatus.CANCELLED);
        Transaction trxRejexted = TransactionFaker.mockInstance(2, SyncTrxStatus.REJECTED);
        when(transactionRepository.findByUserIdAndInitiativeIdAndChannel(USER_ID, INITIATIVE_ID, TRX_CHANNEL_BARCODE))
                .thenReturn(List.of(trxCancelled, trxRejexted));

        //When
        TransactionNotFoundOrExpiredException errorResult = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () -> retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID));

        //Then
        Assertions.assertNotNull(errorResult);
        Assertions.assertEquals("No active transaction found for user", errorResult.getMessage());
    }

    @Test
    void findOldestNoAuthorized_initiativeNotFound(){
        // Given
        when(rewardRuleRepository.findById(INITIATIVE_ID)).thenReturn(Optional.ofNullable(null));

        //When
        InitiativeNotfoundException errorResult = Assertions.assertThrows(InitiativeNotfoundException.class, () -> retrieveActiveBarcode.findOldestNotAuthorized(USER_ID, INITIATIVE_ID));

        //Then
        Assertions.assertNotNull(errorResult);
        Assertions.assertEquals("Cannot find initiative with id [%s]".formatted(INITIATIVE_ID), errorResult.getMessage());
    }
}