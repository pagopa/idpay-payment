package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointOfSaleTransactionServiceTest {

    @Mock
    private TransactionInProgressRepository repositoryMock;
    @Mock
    private PDVService pdvService;

    private PointOfSaleTransactionService pointOfSaleTransactionService;

    @BeforeEach
    void setUp() {
        pointOfSaleTransactionService = new PointOfSaleTransactionServiceImpl(repositoryMock, pdvService);
    }

    @Test
    void getPointOfSaleTransactionList() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction2.setUserId("USERID1");

        Page<TransactionInProgress> expectedPage = new PageImpl<>(List.of(transaction1, transaction2));

        when(pdvService.encryptCF("USERFISCALCODE1")).thenReturn("USERID1");
        when(repositoryMock.findPageByFilter(
                anyString(), anyString(), anyString(), any(), any(), any(Pageable.class)
        )).thenReturn(expectedPage);

        Page<TransactionInProgress> resultPage = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "USERFISCALCODE1", null, Pageable.unpaged());


        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals("USERID1", resultPage.getContent().get(0).getUserId());
        assertEquals("USERID1", resultPage.getContent().get(1).getUserId());
        verify(pdvService).encryptCF("USERFISCALCODE1");
        verify(repositoryMock).findPageByFilter("MERCHANTID1", "POINTOFSALEID1", "INITIATIVEID1", "USERID1", null, Pageable.unpaged());
    }

    @Test
    void getPointOfSaleTransactionList_noFiscalCodeFilter() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");

        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        transaction2.setUserId("USERID2");

        Page<TransactionInProgress> expectedPage = new PageImpl<>(List.of(transaction1, transaction2));

        when(repositoryMock.findPageByFilter(
                anyString(), anyString(), anyString(), isNull(), any(), any(Pageable.class)
        )).thenReturn(expectedPage);

        Page<TransactionInProgress> resultPage = pointOfSaleTransactionService.getPointOfSaleTransactions(
                "MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", null, null, Pageable.unpaged());

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals("USERID1", resultPage.getContent().get(0).getUserId());
        assertEquals("USERID2", resultPage.getContent().get(1).getUserId());
        verify(pdvService, never()).encryptCF(any());
        verify(repositoryMock).findPageByFilter("MERCHANTID1", "POINTOFSALEID1", "INITIATIVEID1", null, null, Pageable.unpaged());
    }

    @Test
    void getPointOfSaleTransactionList_EmptyTransactionInProgressList() {
        when(pdvService.encryptCF("USERFISCALCODE1")).thenReturn("USERID1");

        Page<TransactionInProgress> emptyPage = Page.empty();
        when(repositoryMock.findPageByFilter(
                anyString(), anyString(), anyString(), any(), any(), any(Pageable.class)
        )).thenReturn(emptyPage);

        Page<TransactionInProgress> resultPage = pointOfSaleTransactionService.getPointOfSaleTransactions(
                "MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "USERFISCALCODE1", null, Pageable.unpaged());

        assertNotNull(resultPage);
        assertTrue(resultPage.isEmpty());
        assertEquals(0, resultPage.getTotalElements());

        verify(pdvService).encryptCF("USERFISCALCODE1");
        verify(repositoryMock).findPageByFilter("MERCHANTID1", "POINTOFSALEID1", "INITIATIVEID1", "USERID1", null, Pageable.unpaged());
    }
}
