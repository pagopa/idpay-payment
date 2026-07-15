package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointOfSaleTransactionServiceTest {

    @Mock
    private TransactionService transactionService;

    private PointOfSaleTransactionService pointOfSaleTransactionService;

    @BeforeEach
    void setUp() {
        pointOfSaleTransactionService = new PointOfSaleTransactionServiceImpl(transactionService);
    }

    @Test
    void getPointOfSaleTransactionList_shouldDelegateToTransactionService() {
        Transaction transaction1 = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        Transaction transaction2 = TransactionFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);

        Page<Transaction> expectedPage = new PageImpl<>(List.of(transaction1, transaction2));
        TrxFiltersDTO filters = new TrxFiltersDTO();

        when(transactionService.getTransactionsByFilters(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Transaction> resultPage = pointOfSaleTransactionService.getPointOfSaleTransactions(filters, Pageable.unpaged());

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(transaction1.getId(), resultPage.getContent().get(0).getId());
        assertEquals(transaction2.getId(), resultPage.getContent().get(1).getId());
        verify(transactionService).getTransactionsByFilters(filters, Pageable.unpaged());
    }
}
