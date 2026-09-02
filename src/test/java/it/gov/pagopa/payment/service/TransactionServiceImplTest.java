package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.TransactionServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PDVService pdvService;
    @Mock
    private TrxCodeGenUtil trxCodeGenUtil;
    @Mock
    private AppConfigurationProperties.ExtendedTransactions extendedTransactions;
    @Mock
    private TransactionNotifierService transactionNotifierService;

    private TransactionServiceImpl transactionService;

    private static final String INITIATIVE_ID = "INITIATIVE_1";
    private static final String USER_ID = "USER_1";
    private static final String TRX_ID = "TRX_1";
    private static final String MERCHANT_ID = "MERCHANT_1";
    private static final String FISCAL_CODE = "AAABBB00A00A000A";
    private static final String ENCRYPTED_CF = "ENCRYPTED_CF";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                pdvService,
                trxCodeGenUtil,
                extendedTransactions,
                transactionNotifierService,
                extendedTransactions
        );
    }

    // =========================================================================
    // 1. GENERATE TRX CODE AND SAVE
    // =========================================================================

    @Test
    @DisplayName("generateTrxCodeAndSave - Successo al primo tentativo")
    void testGenerateTrxCodeAndSave_FirstTrySuccess() {
        Transaction trx = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        when(trxCodeGenUtil.get()).thenReturn("CODE123");
        when(transactionRepository.existsByTrxCode("CODE123")).thenReturn(false);

        transactionService.generateTrxCodeAndSave(trx, "TEST_FLOW");

        assertEquals("CODE123", trx.getTrxCode());
        verify(transactionRepository).save(trx);
    }

    @Test
    @DisplayName("generateTrxCodeAndSave - Gestione collisione codice e retry")
    void testGenerateTrxCodeAndSave_RetryOnDuplicate() {
        Transaction trx = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        when(trxCodeGenUtil.get()).thenReturn("DUPLICATE_CODE", "UNIQUE_CODE");
        when(transactionRepository.existsByTrxCode("DUPLICATE_CODE")).thenReturn(true);
        when(transactionRepository.existsByTrxCode("UNIQUE_CODE")).thenReturn(false);

        transactionService.generateTrxCodeAndSave(trx, "TEST_FLOW");

        assertEquals("UNIQUE_CODE", trx.getTrxCode());
        verify(transactionRepository, times(2)).existsByTrxCode(anyString());
        verify(transactionRepository).save(trx);
    }

    // =========================================================================
    // 2. GET TRANSACTIONS BY FILTERS
    // =========================================================================

    @Test
    @DisplayName("getTransactionsByFilters - Filtri null (TransactionMissingParametersException)")
    void testGetTransactionsByFilters_NullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.getTransactionsByFilters(null, pageable));
    }

    @Test
    @DisplayName("getTransactionsByFilters - Successo con cifratura CF")
    void testGetTransactionsByFilters_Success() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setFiscalCode(FISCAL_CODE);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        when(pdvService.encryptCF(FISCAL_CODE)).thenReturn(ENCRYPTED_CF);

        try (MockedStatic<TransactionSpecifications> specMock = mockStatic(TransactionSpecifications.class)) {
            Specification<Transaction> spec = mock(Specification.class);
            specMock.when(() -> TransactionSpecifications.buildSpecification(filters, ENCRYPTED_CF)).thenReturn(spec);
            when(transactionRepository.findAll(spec, pageable)).thenReturn(page);

            Page<Transaction> result = transactionService.getTransactionsByFilters(filters, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(pdvService).encryptCF(FISCAL_CODE);
        }
    }

    // =========================================================================
    // 3. GET TRANSACTION BY ID AND MERCHANT ID
    // =========================================================================

    @Test
    @DisplayName("getTransactionByIdAndMerchantId - Parametri mancanti")
    void testGetTransactionByIdAndMerchantId_MissingParameters() {
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId("", ""));
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId(TRX_ID, ""));
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId("", MERCHANT_ID));
    }

    @Test
    @DisplayName("getTransactionByIdAndMerchantId - Transazione non trovata")
    void testGetTransactionByIdAndMerchantId_NotFound() {
        when(transactionRepository.findByIdAndMerchantIdAndStatusIn(eq(TRX_ID), eq(MERCHANT_ID), anyList()))
                .thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> transactionService.getTransactionByIdAndMerchantId(TRX_ID, MERCHANT_ID));
    }

    @Test
    @DisplayName("getTransactionByIdAndMerchantId - Successo")
    void testGetTransactionByIdAndMerchantId_Success() {
        Transaction trx = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        trx.setId(TRX_ID);
        when(transactionRepository.findByIdAndMerchantIdAndStatusIn(eq(TRX_ID), eq(MERCHANT_ID), anyList()))
                .thenReturn(Optional.of(trx));

        Transaction result = transactionService.getTransactionByIdAndMerchantId(TRX_ID, MERCHANT_ID);

        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
    }

    // =========================================================================
    // 4. FIND ALL (RICERCA TRANSAZIONI)
    // =========================================================================

    @Test
    @DisplayName("findAll - Ricerca per idTrxIssuer")
    void testFindAll_ByIdTrxIssuer() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        try (MockedStatic<TransactionSpecifications> specMock = mockStatic(TransactionSpecifications.class)) {
            Specification<Transaction> spec = mock(Specification.class);
            specMock.when(() -> TransactionSpecifications.findByIssuerFilters("ISSUER_1", USER_ID, null, null, 100L))
                    .thenReturn(spec);
            when(transactionRepository.findAll(spec, pageable)).thenReturn(page);

            List<Transaction> result = transactionService.findAll("ISSUER_1", USER_ID, null, null, 100L, pageable);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("findAll - Ricerca per Range di date e userId")
    void testFindAll_ByRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        try (MockedStatic<TransactionSpecifications> specMock = mockStatic(TransactionSpecifications.class)) {
            Specification<Transaction> spec = mock(Specification.class);
            specMock.when(() -> TransactionSpecifications.findByRangeFilters(USER_ID, now, now, 100L))
                    .thenReturn(spec);
            when(transactionRepository.findAll(spec, pageable)).thenReturn(page);

            List<Transaction> result = transactionService.findAll(null, USER_ID, now, now, 100L, pageable);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("findAll - Parametri obbligatori parzialmente mancanti")
    void testFindAll_MissingPartialParameters() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();

        // Manca userId
        TransactionMissingParametersException ex1 = assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, null, now, now, null, pageable));
        assertTrue(ex1.getMessage().contains("userId"));

        // Manca trxDateStart
        TransactionMissingParametersException ex2 = assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, USER_ID, null, now, null, pageable));
        assertTrue(ex2.getMessage().contains("trxDateStart"));

        // Manca trxDateEnd
        TransactionMissingParametersException ex3 = assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, USER_ID, now, null, null, pageable));
        assertTrue(ex3.getMessage().contains("trxDateEnd"));
    }

    @Test
    @DisplayName("findAll - Tutti e 3 i filtri principali mancanti (aggiunge idTrxIssuer alla lista)")
    void testFindAll_AllThreeFiltersMissing() {
        Pageable pageable = PageRequest.of(0, 10);

        TransactionMissingParametersException ex = assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, null, null, null, null, pageable));

        assertTrue(ex.getMessage().contains("idTrxIssuer"));
    }

    // =========================================================================
    // 5. FIND BY INITIATIVE ID AND USER ID
    // =========================================================================

    @Test
    @DisplayName("findByInitiativeIdAndUserId - Parametri mancanti")
    void testFindByInitiativeIdAndUserId_MissingParams() {
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("", ""));
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId(INITIATIVE_ID, ""));
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("", USER_ID));
    }

    @Test
    @DisplayName("findByInitiativeIdAndUserId - Successo")
    void testFindByInitiativeIdAndUserId_Success() {
        try (MockedStatic<TransactionSpecifications> specMock = mockStatic(TransactionSpecifications.class)) {
            Specification<Transaction> spec = mock(Specification.class);
            specMock.when(() -> TransactionSpecifications.findByInitiativeAndUser(INITIATIVE_ID, USER_ID)).thenReturn(spec);
            when(transactionRepository.findAll(spec)).thenReturn(List.of(new Transaction()));

            List<Transaction> result = transactionService.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // =========================================================================
    // 6. GET MERCHANT TRANSACTION BY FILTER
    // =========================================================================

    @Test
    @DisplayName("getMerchantTransactionByFilter - Successo con CF vuoto")
    void testGetMerchantTransactionByFilter_Success_BlankFiscalCode() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        try (MockedStatic<TransactionSpecifications> specMock = mockStatic(TransactionSpecifications.class)) {
            Specification<Transaction> spec = mock(Specification.class);
            specMock.when(() -> TransactionSpecifications.getFilters(filters, null)).thenReturn(spec);
            when(transactionRepository.findAll(spec, pageable)).thenReturn(page);

            Page<Transaction> result = transactionService.getMerchantTransactionByFilter(filters, pageable);

            assertNotNull(result);
            verify(pdvService, never()).encryptCF(any());
        }
    }

    // =========================================================================
    // 7. FIND AND UPDATE EXPIRED TRANSACTIONS STATUS
    // =========================================================================

    @Test
    @DisplayName("findAndUpdateExpiredTransactionsStatus - Successo")
    void testFindAndUpdateExpiredTransactionsStatus_Success() {
        when(transactionRepository.updateStatusForExpiredVoucherTransactions(eq(INITIATIVE_ID), any(OffsetDateTime.class)))
                .thenReturn(5);

        long count = transactionService.findAndUpdateExpiredTransactionsStatus(INITIATIVE_ID);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("findAndUpdateExpiredTransactionsStatus - Eccezione DB (ExpirationStatusUpdateException)")
    void testFindAndUpdateExpiredTransactionsStatus_Exception() {
        when(transactionRepository.updateStatusForExpiredVoucherTransactions(anyString(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        assertThrows(ExpirationStatusUpdateException.class,
                () -> transactionService.findAndUpdateExpiredTransactionsStatus(INITIATIVE_ID));
    }

    // =========================================================================
    // 8. SEND EVENT FOR STALE EXPIRED TRANSACTIONS
    // =========================================================================

    @Test
    @DisplayName("sendEventForStaleExpiredTransactions - Successo con un ciclo batch")
    void testSendEventForStaleExpiredTransactions_SuccessSingleBatch() {
        Transaction trx = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        trx.setId(TRX_ID);

        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(30);
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(10);
        when(transactionRepository.findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                eq(INITIATIVE_ID), eq(SyncTrxStatus.EXPIRED), any(OffsetDateTime.class), any(Pageable.class)
        )).thenReturn(List.of(trx));

        when(transactionNotifierService.notify(trx, TRX_ID)).thenReturn(true);

        long count = transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID);

        assertEquals(1, count);
        verify(transactionNotifierService).notify(trx, TRX_ID);
    }

    @Test
    @DisplayName("sendEventForStaleExpiredTransactions - Successo con cicli multipli fino a lista vuota")
    void testSendEventForStaleExpiredTransactions_SuccessMultipleBatches() {
        Transaction trx1 = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        trx1.setId("TRX_1");
        Transaction trx2 = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        trx2.setId("TRX_2");

        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(30);
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(2);

        when(transactionRepository.findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                eq(INITIATIVE_ID), eq(SyncTrxStatus.EXPIRED), any(OffsetDateTime.class), any(Pageable.class)
        )).thenReturn(List.of(trx1, trx2)).thenReturn(Collections.emptyList());

        when(transactionNotifierService.notify(any(), anyString())).thenReturn(true);

        long count = transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID);

        assertEquals(2, count);
        verify(transactionNotifierService, times(2)).notify(any(), anyString());
    }

    @Test
    @DisplayName("sendEventForStaleExpiredTransactions - Fallimento invio notifica (ExpirationStatusUpdateException)")
    void testSendEventForStaleExpiredTransactions_NotifyFailure() {
        Transaction trx = TransactionFaker.mockInstance(1,SyncTrxStatus.CREATED);
        trx.setId(TRX_ID);

        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(30);
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(10);
        when(transactionRepository.findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                eq(INITIATIVE_ID), eq(SyncTrxStatus.EXPIRED), any(OffsetDateTime.class), any(Pageable.class)
        )).thenReturn(List.of(trx));

        when(transactionNotifierService.notify(trx, TRX_ID)).thenReturn(false);

        assertThrows(ExpirationStatusUpdateException.class,
                () -> transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID));
    }

    @Test
    @DisplayName("sendEventForStaleExpiredTransactions - ExpirationStatusUpdateException diretta rilanciata")
    void testSendEventForStaleExpiredTransactions_DirectExpirationException() {
        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(30);
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(10);
        when(transactionRepository.findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                any(), any(), any(), any()
        )).thenThrow(new ExpirationStatusUpdateException("Custom Exception"));

        ExpirationStatusUpdateException ex = assertThrows(ExpirationStatusUpdateException.class,
                () -> transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID));

        assertEquals("Custom Exception", ex.getMessage());
    }

    @Test
    @DisplayName("sendEventForStaleExpiredTransactions - Eccezione generica impacchettata in ExpirationStatusUpdateException")
    void testSendEventForStaleExpiredTransactions_GenericException() {
        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(30);
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(10);
        when(transactionRepository.findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                any(), any(), any(), any()
        )).thenThrow(new RuntimeException("Generic DB Error"));

        ExpirationStatusUpdateException ex = assertThrows(ExpirationStatusUpdateException.class,
                () -> transactionService.sendEventForStaleExpiredTransactions(INITIATIVE_ID));

        assertEquals("Generic DB Error", ex.getMessage());
    }

    // =========================================================================
    // 9. UPDATE TRANSACTIONS STATUS
    // =========================================================================

    @Test
    @DisplayName("updateTransactionsStatus - Successo")
    void testUpdateTransactionsStatus_Success() {
        when(transactionRepository.bulkUpdateStatusByIds(
                eq(List.of("TRX_1", "TRX_2")),
                eq(SyncTrxStatus.REWARDED),
                any(LocalDateTime.class)
        )).thenReturn(2);

        int updated = transactionService.updateTransactionsStatus(
                List.of("TRX_1", "TRX_2", "TRX_1"),
                SyncTrxStatus.REWARDED
        );

        assertEquals(2, updated);
        verify(transactionRepository, times(1)).bulkUpdateStatusByIds(
                eq(List.of("TRX_1", "TRX_2")),
                eq(SyncTrxStatus.REWARDED),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("updateTransactionsStatus - Parametri mancanti")
    void testUpdateTransactionsStatus_MissingParameters() {
        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.updateTransactionsStatus(null, SyncTrxStatus.REWARDED));

        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.updateTransactionsStatus(List.of(), SyncTrxStatus.REWARDED));

        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.updateTransactionsStatus(List.of("TRX_1"), null));

        assertThrows(TransactionMissingParametersException.class,
                () -> transactionService.updateTransactionsStatus(List.of(" ", "\t"), SyncTrxStatus.REWARDED));

        verify(transactionRepository, never()).bulkUpdateStatusByIds(anyList(), any(), any());
    }
}