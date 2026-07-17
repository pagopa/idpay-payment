package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.TRANSACTIONS_MISSING_MANDATORY_FILTERS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PDVService pdvService;

    private TransactionServiceImpl transactionService;

    private MockedStatic<TransactionSpecifications> specificationsMockedStatic;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, pdvService);
        // Mock statico delle specifiche per isolare i test dall'effettiva compilazione dei criteri SQL
        specificationsMockedStatic = Mockito.mockStatic(TransactionSpecifications.class);
    }

    @AfterEach
    void tearDown() {
        specificationsMockedStatic.close();
    }

    // =========================================================================
    // TEST: getTransactionsByFilters
    // =========================================================================

    @Test
    void getTransactionsByFilters_success() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setFiscalCode("CF_123");
        Pageable pageable = PageRequest.of(0, 10);

        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> expectedPage = new PageImpl<>(List.of(new Transaction()));

        when(pdvService.encryptCF("CF_123")).thenReturn("ENCRYPTED_CF");
        specificationsMockedStatic.when(() -> TransactionSpecifications.buildSpecification(filters, "ENCRYPTED_CF"))
                .thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getTransactionsByFilters(filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(pdvService, times(1)).encryptCF("CF_123");
    }

    @Test
    void getTransactionsByFilters_nullFilters_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.getTransactionsByFilters(null, Pageable.unpaged())
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("Missing mandatory filters: filters"));
    }

    // =========================================================================
    // TEST: getTransactionByIdAndMerchantId
    // =========================================================================

    @Test
    void getTransactionByIdAndMerchantId_success() {
        String txId = "TX_1";
        String merchantId = "M_1";
        Transaction expectedTx = new Transaction();
        expectedTx.setId(txId);

        when(transactionRepository.findByIdAndMerchantIdAndStatusIn(
                eq(txId), eq(merchantId), any()
        )).thenReturn(Optional.of(expectedTx));

        Transaction result = transactionService.getTransactionByIdAndMerchantId(txId, merchantId);

        assertNotNull(result);
        assertEquals(txId, result.getId());
    }

    @Test
    void getTransactionByIdAndMerchantId_missingParameters_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId("", null)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("transactionId"));
        assertTrue(exception.getMessage().contains("merchantId"));
    }

    @Test
    void getTransactionByIdAndMerchantId_notFound_throwsException() {
        String txId = "TX_NOT_FOUND";
        String merchantId = "M_1";

        when(transactionRepository.findByIdAndMerchantIdAndStatusIn(
                eq(txId), eq(merchantId), any()
        )).thenReturn(Optional.empty());

        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> transactionService.getTransactionByIdAndMerchantId(txId, merchantId)
        );

        assertTrue(exception.getMessage().contains(txId));
    }

    // =========================================================================
    // TEST: findAll (Regole di instradamento e validazione complessa)
    // =========================================================================

    @Test
    void findAll_routeTo_findByIdTrxIssuer() {
        String idTrxIssuer = "ISSUER_123";
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> dummyPage = new PageImpl<>(List.of(new Transaction()));

        specificationsMockedStatic.when(() -> TransactionSpecifications.findByIssuerFilters(
                eq(idTrxIssuer), any(), any(), any(), any()
        )).thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(dummyPage);

        List<Transaction> result = transactionService.findAll(idTrxIssuer, null, null, null, null, pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void findAll_routeTo_findByRange() {
        String userId = "USER_123";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> dummyPage = new PageImpl<>(List.of(new Transaction()));

        specificationsMockedStatic.when(() -> TransactionSpecifications.findByRangeFilters(
                eq(userId), eq(start), eq(end), any()
        )).thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(dummyPage);

        List<Transaction> result = transactionService.findAll(null, userId, start, end, null, pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void findAll_missingUserIdAndRange_throwsDynamicException() {
        // Nessun parametro compilato: deve segnalare che mancano i filtri minimi sia del Flusso 1 che del Flusso 2
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, null, null, null, null, Pageable.unpaged())
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        // Se non viene specificato idTrxIssuer, si aspetta i dati del Flusso 2.
        // Se mancano tutti e 3 (userId, start, end) l'eccezione dinamica li inserisce tutti, incluso "idTrxIssuer" come alternativa
        assertTrue(exception.getMessage().contains("idTrxIssuer"));
        assertTrue(exception.getMessage().contains("userId"));
        assertTrue(exception.getMessage().contains("trxDateStart"));
        assertTrue(exception.getMessage().contains("trxDateEnd"));
    }

    @Test
    void findAll_partialRangeProvided_throwsDynamicException() {
        // userId presente, ma mancano le date
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, "USER_123", null, null, null, Pageable.unpaged())
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("userId")); // userId c'è, quindi non dev'essere segnalato
        assertTrue(exception.getMessage().contains("trxDateStart"));
        assertTrue(exception.getMessage().contains("trxDateEnd"));
    }

    // =========================================================================
    // TEST: findByInitiativeIdAndUserId
    // =========================================================================

    @Test
    void findByInitiativeIdAndUserId_success() {
        String initiativeId = "INIT_1";
        String userId = "USER_1";
        Specification<Transaction> dummySpec = mock(Specification.class);
        List<Transaction> expectedList = List.of(new Transaction());

        specificationsMockedStatic.when(() -> TransactionSpecifications.findByInitiativeAndUser(initiativeId, userId))
                .thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec)).thenReturn(expectedList);

        List<Transaction> result = transactionService.findByInitiativeIdAndUserId(initiativeId, userId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findByInitiativeIdAndUserId_missingParameters_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("", " ")
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("initiativeId"));
        assertTrue(exception.getMessage().contains("userId"));
    }

    // =========================================================================
    // TEST: getMerchantTransactionByFilter
    // =========================================================================

    @Test
    void getMerchantTransactionByFilter_success() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setFiscalCode("CF_456");
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> expectedPage = new PageImpl<>(Collections.emptyList());

        when(pdvService.encryptCF("CF_456")).thenReturn("ENCRYPTED_CF");
        specificationsMockedStatic.when(() -> TransactionSpecifications.getFilters(filters, "ENCRYPTED_CF"))
                .thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getMerchantTransactionByFilter(filters, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pdvService, times(1)).encryptCF("CF_456");
    }
}