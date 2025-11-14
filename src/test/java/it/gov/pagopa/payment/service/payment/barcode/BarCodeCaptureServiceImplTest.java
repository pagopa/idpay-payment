package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeCaptureServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionBarCodeInProgress2TransactionResponseMapper mapper;

    BarCodeCaptureServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new BarCodeCaptureServiceImpl(
                        repositoryMock,
                        mapper,
                        auditUtilitiesMock);
    }

    @Test
    void testCapturePaymentTrxNotFound() {
        try {
            service.capturePayment("trxCode");
            Assertions.fail("Expected exception");
        } catch (TransactionNotFoundOrExpiredException e) {
            Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", e.getCode());
            Assertions.assertEquals("Cannot find transaction with transactionCode [trxCode]", e.getMessage());
        }
    }

    @Test
    void testCapturePaymentStatusNotValid() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.CREATED);
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));
        try {
            service.capturePayment("trxCode");
            Assertions.fail("Expected exception");
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("Cannot operate on transaction with transactionCode [trxCode] in status CREATED", e.getMessage());
        }
    }

    @Test
    void testCapturePayment() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.AUTHORIZED);
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        TransactionBarCodeResponse result = service.capturePayment("trxCode");

        Assertions.assertEquals(result, mapper.apply(trx));
    }

    @Test
    void capturePayment_deletesWebVoucher_whenTransactionIsApp() {
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(false);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        TransactionInProgress trxOther = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trxOther.setExtendedAuthorization(true);
        trxOther.setUserId("USER01");
        trxOther.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxCurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
            trxCurrent.getUserId(),
            trxCurrent.getInitiativeId(),
            SyncTrxStatus.CREATED,
            trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of(trxOther));
        doNothing().when(repositoryMock).deleteAll(anyList());
        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("trxCurrent");

        assertNotNull(response);
        verify(repositoryMock).deleteAll(List.of(trxOther));
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void capturePayment_deletesAppVoucher_whenTransactionIsWeb() {
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(true);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        TransactionInProgress trxOther = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trxOther.setExtendedAuthorization(false);
        trxOther.setUserId("USER01");
        trxOther.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxCurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
            trxCurrent.getUserId(),
            trxCurrent.getInitiativeId(),
            SyncTrxStatus.CREATED,
            trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of(trxOther));
        doNothing().when(repositoryMock).deleteAll(anyList());
        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("trxCurrent");

        assertNotNull(response);
        verify(repositoryMock).deleteAll(List.of(trxOther));
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void capturePayment_noUnusedVouchers_deleteNotCalled() {
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(false);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxCurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
            trxCurrent.getUserId(),
            trxCurrent.getInitiativeId(),
            SyncTrxStatus.CREATED,
            trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of());

        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("trxCurrent");

        assertNotNull(response);
        verify(repositoryMock, never()).deleteAll(anyList());
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void retriveVoucher_ok() {
        String initiativeId = "INIT1";
        String trxCode = "TRX123";
        String userId = "USR1";

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("id-1");
        trx.setInitiativeId(initiativeId);
        trx.setTrxCode(trxCode);
        trx.setUserId(userId);
        trx.setRewardCents(100L);

        TransactionBarCodeResponse expected = new TransactionBarCodeResponse();

        when(repositoryMock.findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId))
                .thenReturn(Optional.of(trx));
        when(mapper.apply(trx)).thenReturn(expected);

        TransactionBarCodeResponse result = service.retriveVoucher(initiativeId, trxCode, userId);

        assertSame(expected, result);

        verify(repositoryMock).findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId);
        verify(mapper).apply(trx);
        verify(auditUtilitiesMock).logRetriveVoucher(
                trx.getInitiativeId(),
                trx.getId(),
                trx.getTrxCode(),
                trx.getUserId(),
                trx.getRewardCents(),
                trx.getRejectionReasons()
        );
        verify(auditUtilitiesMock, never()).logErrorRetriveVoucher(any(), any(), any());
        verifyNoMoreInteractions(auditUtilitiesMock);
    }

    @Test
    void retriveVoucher_notFound_logsAndThrows() {
        String initiativeId = "INIT1";
        String trxCode = "TRX404";
        String userId = "USR1";

        when(repositoryMock.findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId))
                .thenReturn(Optional.empty());

        TransactionNotFoundOrExpiredException ex = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> service.retriveVoucher(initiativeId, trxCode, userId)
        );
        assertTrue(ex.getMessage().contains(trxCode));

        verify(repositoryMock).findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId);
        verify(auditUtilitiesMock).logErrorRetriveVoucher(initiativeId, trxCode, userId);
        verify(auditUtilitiesMock, never()).logRetriveVoucher(any(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(auditUtilitiesMock);
        verifyNoInteractions(mapper);
    }

    @Test
    void retriveVoucher_mapperThrows_logsSuccessThenError_andRethrows() {
        String initiativeId = "INIT1";
        String trxCode = "TRX123";
        String userId = "USR1";

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("ID-1");
        trx.setInitiativeId(initiativeId);
        trx.setTrxCode(trxCode);
        trx.setUserId(userId);
        trx.setRewardCents(100L);

        when(repositoryMock.findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId))
                .thenReturn(Optional.of(trx));
        when(mapper.apply(trx)).thenThrow(new IllegalStateException("boom"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.retriveVoucher(initiativeId, trxCode, userId)
        );
        assertEquals("boom", ex.getMessage());

        verify(repositoryMock).findByInitiativeIdAndTrxCodeAndUserId(initiativeId, trxCode, userId);
        verify(mapper).apply(trx);

        InOrder inOrder = inOrder(auditUtilitiesMock);
        inOrder.verify(auditUtilitiesMock).logRetriveVoucher(
                eq(trx.getInitiativeId()),
                eq(trx.getId()),
                eq(trx.getTrxCode()),
                eq(trx.getUserId()),
                eq(trx.getRewardCents()),
                (java.util.List<String>) nullable(java.util.List.class)
        );
        inOrder.verify(auditUtilitiesMock).logErrorRetriveVoucher(initiativeId, trxCode, userId);
    }
}
