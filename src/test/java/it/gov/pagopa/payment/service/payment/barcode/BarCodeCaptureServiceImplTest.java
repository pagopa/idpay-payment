package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.PointOfSaleNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeCaptureServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionBarCodeInProgress2TransactionResponseMapper mapper;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSynchronizer transactionSynchronizer;
    @Mock private MerchantConnector merchantConnector;

    BarCodeCaptureServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new BarCodeCaptureServiceImpl(
                        transactionRepository,
                        repositoryMock,
                        mapper,
                        auditUtilitiesMock,
                        transactionSynchronizer,
                        merchantConnector);
    }

    @Test
    void testCapturePaymentTrxNotFound() {
        TransactionNotFoundOrExpiredException exception = Assertions.assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> service.capturePayment("initiativeId", "trxCode", "merchantId", "pointOfSaleId", "acquirerId")
        );

        Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", exception.getCode());
        Assertions.assertEquals("Cannot find transaction with transactionCode [trxCode]", exception.getMessage());
    }

    @Test
    void testCapturePaymentStatusNotValid() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);

        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.CREATED);

        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        OperationNotAllowedException exception = Assertions.assertThrows(
                OperationNotAllowedException.class,
                () -> service.capturePayment("initiativeId", "trxCode", "merchantId", "pointOfSaleId", "acquirerId")
        );

        Assertions.assertEquals(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, exception.getCode());
        Assertions.assertEquals("Cannot operate on transaction with transactionCode [trxCode] in status CREATED", exception.getMessage());
        verifyNoInteractions(merchantConnector);
    }

    @Test
    void testCapturePayment() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setInitiativeId("INITIATIVEID");
        trx.setMerchantId("MERCHID");
        trx.setPointOfSaleId("POSID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.AUTHORIZED);
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));
        when(merchantConnector.merchantDetail("MERCHID", "INITIATIVEID")).thenReturn(MerchantDetailDTO.builder().initiativeId("INITIATIVEID").build());
        when(merchantConnector.getPointOfSale("MERCHID", "POSID", "INITIATIVEID")).thenReturn(PointOfSaleDTO.builder().businessName("Business").build());

        TransactionBarCodeResponse result = service.capturePayment("INITIATIVEID", "trxCode", "MERCHID", "POSID", "ACQID");

        Assertions.assertEquals(result, mapper.apply(trx));
        verify(merchantConnector).merchantDetail("MERCHID", "INITIATIVEID");
        verify(merchantConnector).getPointOfSale("MERCHID", "POSID", "INITIATIVEID");
    }

    @Test
    void capturePayment_initiativeMismatch_throwsException() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setInitiativeId("INITIATIVE_ASSOCIATED");
        trx.setMerchantId("MERCHID");
        trx.setPointOfSaleId("POSID");
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        InitiativeNotfoundException exception = assertThrows(
                InitiativeNotfoundException.class,
                () -> service.capturePayment("INITIATIVE_REQUEST", "trxCode", "MERCHID", "POSID", "ACQID")
        );

        assertEquals(ExceptionCode.INITIATIVE_NOT_FOUND, exception.getCode());
        assertEquals(
                "The initiative with id [INITIATIVE_ASSOCIATED] associated to the transaction is not equal to the initiative with id [INITIATIVE_REQUEST]",
                exception.getMessage());
        verifyNoInteractions(merchantConnector);
    }

    @Test
    void capturePayment_merchantMismatch_throwsException() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setInitiativeId("INITIATIVEID");
        trx.setMerchantId("MERCHANT_ASSOCIATED");
        trx.setPointOfSaleId("POSID");
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        MerchantOrAcquirerNotAllowedException exception = assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> service.capturePayment("INITIATIVEID", "trxCode", "MERCHANT_REQUEST", "POSID", "ACQID")
        );

        assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, exception.getCode());
        assertEquals(
                "The merchant with id [MERCHANT_ASSOCIATED] associated to the transaction is not equal to the merchant with id [MERCHANT_REQUEST]",
                exception.getMessage());
        verifyNoInteractions(merchantConnector);
    }

    @Test
    void capturePayment_pointOfSaleMismatch_throwsException() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setInitiativeId("INITIATIVEID");
        trx.setMerchantId("MERCHID");
        trx.setPointOfSaleId("POS_ASSOCIATED");
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        PointOfSaleNotAllowedException exception = assertThrows(
                PointOfSaleNotAllowedException.class,
                () -> service.capturePayment("INITIATIVEID", "trxCode", "MERCHID", "POS_REQUEST", "ACQID")
        );

        assertEquals(ExceptionCode.PAYMENT_POS_NOT_ALLOWED, exception.getCode());
        assertEquals(
                "The pointOfSaleId with id [POS_ASSOCIATED] associated to the transaction is not equal to the pointOfSaleId with id [POS_REQUEST]",
                exception.getMessage());
        verifyNoInteractions(merchantConnector);
    }

    @Test
    void capturePayment_deletesWebVoucher_whenTransactionIsApp() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(false);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        TransactionInProgress trxOther = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trxOther.setExtendedAuthorization(true);
        trxOther.setUserId("USER01");
        trxOther.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxcurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                trxCurrent.getUserId(),
                trxCurrent.getInitiativeId(),
                SyncTrxStatus.CREATED,
                trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of(trxOther));
        when(merchantConnector.merchantDetail("MERCHID", "INIT01")).thenReturn(MerchantDetailDTO.builder().initiativeId("INIT01").build());
        when(merchantConnector.getPointOfSale("MERCHID", "POS01", "INIT01")).thenReturn(PointOfSaleDTO.builder().businessName("Business").build());
        trxCurrent.setMerchantId("MERCHID");
        trxCurrent.setPointOfSaleId("POS01");
        doNothing().when(repositoryMock).deleteAll(anyList());
        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("INIT01", "trxcurrent", "MERCHID", "POS01", "ACQID");

        assertNotNull(response);
        verify(repositoryMock).deleteAll(List.of(trxOther));
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void capturePayment_deletesAppVoucher_whenTransactionIsWeb() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(true);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        TransactionInProgress trxOther = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        trxOther.setExtendedAuthorization(false);
        trxOther.setUserId("USER01");
        trxOther.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxcurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                trxCurrent.getUserId(),
                trxCurrent.getInitiativeId(),
                SyncTrxStatus.CREATED,
                trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of(trxOther));
        when(merchantConnector.merchantDetail("MERCHID", "INIT01")).thenReturn(MerchantDetailDTO.builder().initiativeId("INIT01").build());
        when(merchantConnector.getPointOfSale("MERCHID", "POS01", "INIT01")).thenReturn(PointOfSaleDTO.builder().businessName("Business").build());
        trxCurrent.setMerchantId("MERCHID");
        trxCurrent.setPointOfSaleId("POS01");
        doNothing().when(repositoryMock).deleteAll(anyList());
        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("INIT01", "trxcurrent", "MERCHID", "POS01", "ACQID");

        assertNotNull(response);
        verify(repositoryMock).deleteAll(List.of(trxOther));
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void capturePayment_noUnusedVouchers_deleteNotCalled() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCode(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trxCurrent = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trxCurrent.setExtendedAuthorization(false);
        trxCurrent.setUserId("USER01");
        trxCurrent.setInitiativeId("INIT01");

        when(repositoryMock.findByTrxCode("trxcurrent")).thenReturn(Optional.of(trxCurrent));
        when(repositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                trxCurrent.getUserId(),
                trxCurrent.getInitiativeId(),
                SyncTrxStatus.CREATED,
                trxCurrent.getExtendedAuthorization()
        )).thenReturn(List.of());
        when(merchantConnector.merchantDetail("MERCHID", "INIT01")).thenReturn(MerchantDetailDTO.builder().initiativeId("INIT01").build());
        when(merchantConnector.getPointOfSale("MERCHID", "POS01", "INIT01")).thenReturn(PointOfSaleDTO.builder().businessName("Business").build());
        trxCurrent.setMerchantId("MERCHID");
        trxCurrent.setPointOfSaleId("POS01");

        when(repositoryMock.save(trxCurrent)).thenReturn(trxCurrent);
        when(mapper.apply(trxCurrent)).thenReturn(new TransactionBarCodeResponse());

        TransactionBarCodeResponse response = service.capturePayment("INIT01", "trxcurrent", "MERCHID", "POS01", "ACQID");

        assertNotNull(response);
        verify(repositoryMock, never()).deleteAll(anyList());
        verify(repositoryMock).save(trxCurrent);
    }

    @Test
    void retriveVoucher_ok() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(anyString(),any(),any())).thenReturn(Optional.of(transaction));
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
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findByInitiativeIdAndTrxCodeAndUserId(anyString(),any(),any())).thenReturn(Optional.of(transaction));
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
