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
import org.mockito.InjectMocks;
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

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private MockedStatic<TransactionSpecifications> specificationsMockedStatic;

    @BeforeEach
    void setUp() {
        specificationsMockedStatic = Mockito.mockStatic(TransactionSpecifications.class);
    }

    @AfterEach
    void tearDown() {
        specificationsMockedStatic.close();
    }

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
    void getTransactionsByFilters_successWithNullFiscalCode() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setFiscalCode(null);
        Pageable pageable = PageRequest.of(0, 10);

        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> expectedPage = new PageImpl<>(List.of(new Transaction()));

        specificationsMockedStatic.when(() -> TransactionSpecifications.buildSpecification(filters, null))
                .thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getTransactionsByFilters(filters, pageable);

        assertNotNull(result);
        verifyNoInteractions(pdvService);
    }

    @Test
    void getTransactionsByFilters_nullFilters_throwsException() {
        Pageable pageable = Pageable.unpaged();

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.getTransactionsByFilters(null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("Missing mandatory filters: filters"));
    }

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
    void getTransactionByIdAndMerchantId_missingTransactionId_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId(" ", "M_1")
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("transactionId"));
        assertFalse(exception.getMessage().contains("merchantId"));
    }

    @Test
    void getTransactionByIdAndMerchantId_missingMerchantId_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.getTransactionByIdAndMerchantId("TX_1", "")
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("transactionId"));
        assertTrue(exception.getMessage().contains("merchantId"));
    }

    @Test
    void getTransactionByIdAndMerchantId_missingBothParameters_throwsException() {
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
        Pageable pageable = Pageable.unpaged();

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, null, null, null, null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("idTrxIssuer"));
        assertTrue(exception.getMessage().contains("userId"));
        assertTrue(exception.getMessage().contains("trxDateStart"));
        assertTrue(exception.getMessage().contains("trxDateEnd"));
    }

    @Test
    void findAll_partialRangeMissingDates_throwsDynamicException() {
        Pageable pageable = Pageable.unpaged();

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, "USER_123", null, null, null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("idTrxIssuer"));
        assertFalse(exception.getMessage().contains("userId"));
        assertTrue(exception.getMessage().contains("trxDateStart"));
        assertTrue(exception.getMessage().contains("trxDateEnd"));
    }

    @Test
    void findAll_partialRangeMissingUserId_throwsDynamicException() {
        Pageable pageable = Pageable.unpaged();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, " ", start, end, null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("idTrxIssuer"));
        assertTrue(exception.getMessage().contains("userId"));
        assertFalse(exception.getMessage().contains("trxDateStart"));
        assertFalse(exception.getMessage().contains("trxDateEnd"));
    }

    @Test
    void findAll_partialRangeMissingStart_throwsDynamicException() {
        Pageable pageable = Pageable.unpaged();
        LocalDateTime end = LocalDateTime.now();

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll("", "USER_123", null, end, null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("trxDateStart"));
        assertFalse(exception.getMessage().contains("userId"));
        assertFalse(exception.getMessage().contains("trxDateEnd"));
    }

    @Test
    void findAll_partialRangeMissingEnd_throwsDynamicException() {
        Pageable pageable = Pageable.unpaged();
        LocalDateTime start = LocalDateTime.now().minusDays(1);

        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findAll(null, "USER_123", start, null, null, pageable)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("userId"));
        assertFalse(exception.getMessage().contains("trxDateStart"));
        assertTrue(exception.getMessage().contains("trxDateEnd"));
    }

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
    void findByInitiativeIdAndUserId_missingInitiativeId_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("", "USER_1")
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("initiativeId"));
        assertFalse(exception.getMessage().contains("userId"));
    }

    @Test
    void findByInitiativeIdAndUserId_missingUserId_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("INIT_1", "   ")
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertFalse(exception.getMessage().contains("initiativeId"));
        assertTrue(exception.getMessage().contains("userId"));
    }

    @Test
    void findByInitiativeIdAndUserId_missingBothParameters_throwsException() {
        TransactionMissingParametersException exception = assertThrows(
                TransactionMissingParametersException.class,
                () -> transactionService.findByInitiativeIdAndUserId("", null)
        );

        assertEquals(TRANSACTIONS_MISSING_MANDATORY_FILTERS, exception.getCode());
        assertTrue(exception.getMessage().contains("initiativeId"));
        assertTrue(exception.getMessage().contains("userId"));
    }

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

    @Test
    void getMerchantTransactionByFilter_successWithBlankFiscalCode() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setFiscalCode("");
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Transaction> dummySpec = mock(Specification.class);
        Page<Transaction> expectedPage = new PageImpl<>(Collections.emptyList());

        specificationsMockedStatic.when(() -> TransactionSpecifications.getFilters(filters, null))
                .thenReturn(dummySpec);
        when(transactionRepository.findAll(dummySpec, pageable)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getMerchantTransactionByFilter(filters, pageable);

        assertNotNull(result);
        verifyNoInteractions(pdvService);
    }
}