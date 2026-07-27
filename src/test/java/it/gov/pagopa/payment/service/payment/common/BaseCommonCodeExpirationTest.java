package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseCommonCodeExpirationTest {

    @Mock
    private AuditUtilities auditUtilitiesMock;

    private TestCommonCodeExpiration expirationService;

    // Sottoclasse concreta fittizia per testare la classe astratta
    static class TestCommonCodeExpiration extends BaseCommonCodeExpiration {
        private final long expirationMinutes;
        private final String subFlowName;

        public TestCommonCodeExpiration(AuditUtilities auditUtilities, String channel, long expirationMinutes, String subFlowName) {
            super(auditUtilities, channel);
            this.expirationMinutes = expirationMinutes;
            this.subFlowName = subFlowName;
        }

        @Override
        protected long getExpirationMinutes() {
            return expirationMinutes;
        }

        @Override
        protected Transaction findExpiredTransaction(String initiativeId, long expirationMinutes) {
            return null; // Overridden nei test tramite spy/mock
        }

        @Override
        protected Transaction handleExpiredTransaction(Transaction trx) {
            return trx; // Overridden nei test se necessario
        }

        @Override
        protected String getFlowName() {
            return subFlowName;
        }
    }

    private static final String CHANNEL = "QR_CODE";
    private static final String SUB_FLOW_NAME = "CONFIRM_EXPIRATION";
    private static final String INITIATIVE_ID = "INITIATIVE_123";
    private static final long DEFAULT_EXPIRATION_MINUTES = 15L;

    @BeforeEach
    void setUp() {
        expirationService = spy(new TestCommonCodeExpiration(
                auditUtilitiesMock,
                CHANNEL,
                DEFAULT_EXPIRATION_MINUTES,
                SUB_FLOW_NAME
        ));
    }

    @Test
    void testForceExpiration_Success() {
        // Given
        Transaction trx = createDummyTransaction("TRX_1");

        // Prima chiamata restituisce la transazione, la seconda restituisce null per interrompere il ciclo while
        doReturn(trx, (Transaction) null)
                .when(expirationService).findExpiredTransaction(INITIATIVE_ID, 0L);
        doReturn(trx).when(expirationService).handleExpiredTransaction(trx);

        // When
        Long count = expirationService.forceExpiration(INITIATIVE_ID);

        // Then
        assertEquals(1L, count);
        verify(expirationService, times(1)).handleExpiredTransaction(trx);
        verify(auditUtilitiesMock, times(1)).logExpiredTransaction(
                trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), SUB_FLOW_NAME
        );
        verify(auditUtilitiesMock, never()).logErrorExpiredTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void testExecute_NoArgs_Success() {
        // Given
        Transaction trx1 = createDummyTransaction("TRX_1");
        Transaction trx2 = createDummyTransaction("TRX_2");

        doReturn(trx1, trx2, (Transaction) null)
                .when(expirationService).findExpiredTransaction(null, DEFAULT_EXPIRATION_MINUTES);
        doReturn(trx1).when(expirationService).handleExpiredTransaction(trx1);
        doReturn(trx2).when(expirationService).handleExpiredTransaction(trx2);

        // When
        Long count = expirationService.execute();

        // Then
        assertEquals(2L, count);
        verify(expirationService, times(3)).findExpiredTransaction(null, DEFAULT_EXPIRATION_MINUTES);
        verify(expirationService, times(1)).handleExpiredTransaction(trx1);
        verify(expirationService, times(1)).handleExpiredTransaction(trx2);
        verify(auditUtilitiesMock, times(2)).logExpiredTransaction(any(), any(), any(), any(), eq(SUB_FLOW_NAME));
    }

    @Test
    void testExecute_NoExpiredTransactions() {
        // Given
        doReturn(null).when(expirationService).findExpiredTransaction(INITIATIVE_ID, DEFAULT_EXPIRATION_MINUTES);

        // When
        Long count = expirationService.execute(INITIATIVE_ID, DEFAULT_EXPIRATION_MINUTES);

        // Then
        assertEquals(0L, count);
        verify(expirationService, times(1)).findExpiredTransaction(INITIATIVE_ID, DEFAULT_EXPIRATION_MINUTES);
        verify(expirationService, never()).handleExpiredTransaction(any());
        verifyNoInteractions(auditUtilitiesMock);
    }

    @Test
    void testExecute_HandleTransactionThrowsException() {
        // Given
        Transaction trx = createDummyTransaction("TRX_ERROR");

        doReturn(trx, (Transaction) null)
                .when(expirationService).findExpiredTransaction(INITIATIVE_ID, DEFAULT_EXPIRATION_MINUTES);
        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(expirationService).handleExpiredTransaction(trx);

        // When
        Long count = expirationService.execute(INITIATIVE_ID, DEFAULT_EXPIRATION_MINUTES);

        // Then
        assertEquals(0L, count); // Il conteggio non si incrementa se va in eccezione
        verify(expirationService, times(1)).handleExpiredTransaction(trx);
        verify(auditUtilitiesMock, times(1)).logErrorExpiredTransaction(
                trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), SUB_FLOW_NAME
        );
        verify(auditUtilitiesMock, never()).logExpiredTransaction(any(), any(), any(), any(), any());
    }

    private Transaction createDummyTransaction(String id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setTrxCode("TRX_CODE_" + id);
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId("USER_ID");
        transaction.setStatus(SyncTrxStatus.CREATED);
        transaction.setTrxDate(LocalDateTime.now());
        return transaction;
    }
}