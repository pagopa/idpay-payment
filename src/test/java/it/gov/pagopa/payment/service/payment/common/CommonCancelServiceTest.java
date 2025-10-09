package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonCancelServiceTest {

  private final long cancelExpirationMinutes = 5;

  @Mock
  private TransactionInProgressRepository repositoryMock;
  @Mock
  private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock
  private TransactionNotifierService notifierServiceMock;
  @Mock
  private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
  @Mock
  private AuditUtilities auditUtilitiesMock;
  @Mock
  private BarCodeCreationServiceImpl barCodeCreationService;

  private CommonCancelServiceImpl service;

  @BeforeEach
  void init() {
    service =
        new CommonCancelServiceImpl(
            cancelExpirationMinutes,
            repositoryMock,
            rewardCalculatorConnectorMock,
            notifierServiceMock,
            paymentErrorNotifierServiceMock,
            auditUtilitiesMock, barCodeCreationService);
  }

  @Test
  void testTrxNotFound() {
    try {
      service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");
      Assertions.fail("Expected exception");
    } catch (TransactionNotFoundOrExpiredException e) {
      Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", e.getCode());
      Assertions.assertEquals("Cannot find transaction with transactionId [TRXID]", e.getMessage());
    }
  }

  @Test
  void testMerchantIdNotValid() {
    when(repositoryMock.findById("TRXID"))
        .thenReturn(Optional.ofNullable(
            TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED)));

    try {
      service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");
      Assertions.fail("Expected exception");
    } catch (MerchantOrAcquirerNotAllowedException e) {
      Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, e.getCode());
      Assertions.assertEquals(
          "The merchant with id [MERCHANTID0] associated to the transaction is not equal to the merchant with id [MERCHID]",
          e.getMessage());
    }
  }

  @Test
  void testAcquirerIdNotValid() {
    TransactionInProgress trx =
        TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

    try {
      service.cancelTransaction("TRXID", "MERCHID", "ACQID_WRONG", "POSID");
      Assertions.fail("Expected exception");
    } catch (MerchantOrAcquirerNotAllowedException e) {
      Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, e.getCode());
      Assertions.assertEquals(
          "The merchant with id [MERCHID] associated to the transaction is not equal to the merchant with id [MERCHID]",
          e.getMessage());
    }
  }

  @Test
  void testStatusNotValid() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.REWARDED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

    try {
      service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");
      Assertions.fail("Expected exception");
    } catch (OperationNotAllowedException e) {
      Assertions.assertEquals(ExceptionCode.TRX_DELETE_NOT_ALLOWED, e.getCode());
      Assertions.assertEquals("Cannot cancel transaction with transactionId [TRXID]",
          e.getMessage());
    }
  }

  @Test
  void testTrxExpired() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0,
        SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    trx.setTrxDate(OffsetDateTime.now().minusMinutes(cancelExpirationMinutes + 1));
    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

    try {
      service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");
      Assertions.fail("Expected exception");
    } catch (OperationNotAllowedException e) {
      Assertions.assertEquals(ExceptionCode.PAYMENT_TRANSACTION_EXPIRED, e.getCode());
      Assertions.assertEquals("Cannot cancel expired transaction with transactionId [TRXID]",
          e.getMessage());
    }
  }

  @ParameterizedTest
  @CsvSource({
      "CREATED,false",
      "IDENTIFIED,false",
      "AUTHORIZED,false",
      "AUTHORIZED,true",
  })
  void testSuccessful(SyncTrxStatus status, boolean notifyOutcome) {
    TransactionInProgress trx =
        TransactionInProgressFaker.mockInstance(0, status);
    trx.setId("TRXID");
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    trx.setRewardCents(1000L);
    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

    boolean expectedNotify = SyncTrxStatus.AUTHORIZED.equals(status);

    if (expectedNotify) {
      when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenReturn(new AuthPaymentDTO());
      when(notifierServiceMock.notify(trx, trx.getMerchantId())).thenReturn(notifyOutcome);
    }

    service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");

    verify(repositoryMock).deleteById("TRXID");
    if (expectedNotify && !notifyOutcome) {
      verify(paymentErrorNotifierServiceMock)
          .notifyCancelPayment(any(), any(), eq(true), any());
    }
  }

  @Test
  void testRefundIsNull_ShouldSkipNotificationAndDeleteTrx() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0,
        SyncTrxStatus.AUTHORIZED);
    trx.setId("TRXID");
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    trx.setExtendedAuthorization(false);

    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenReturn(null);

    service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");

    verify(repositoryMock).deleteById("TRXID");
    verifyNoInteractions(notifierServiceMock);
    verifyNoInteractions(paymentErrorNotifierServiceMock);
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void testAuthorizedTransaction_WithReset_ShouldResetAndSave() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0,
        SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");
    trx.setExtendedAuthorization(true);

    AuthPaymentDTO refund = new AuthPaymentDTO();
    refund.setRewardCents(1000L);

    when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenReturn(refund);
    when(notifierServiceMock.notify(trx, trx.getUserId())).thenReturn(true);

    service.cancelTransaction("TRXID", "MERCHID", "ACQID", "POSID");
    verify(notifierServiceMock).notify(trx, trx.getUserId());
    verify(repositoryMock, never()).deleteById(any());
    verify(repositoryMock).save(trx);
    Assertions.assertEquals(SyncTrxStatus.CREATED, trx.getStatus());
  }

  @Test
  void testSendCancelledTransactionNotification_NotifyReturnsFalse_ShouldThrowException() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");

    when(notifierServiceMock.notify(trx, trx.getUserId())).thenReturn(false);
    when(notifierServiceMock.buildMessage(trx, trx.getUserId())).thenReturn(Mockito.mock(org.springframework.messaging.Message.class));
    when(paymentErrorNotifierServiceMock.notifyCancelPayment(
        any(), anyString(), eq(true), any(InternalServerErrorException.class)))
        .thenReturn(true);

    ReflectionTestUtils.invokeMethod(service, "sendCancelledTransactionNotification", trx, false);

    verify(notifierServiceMock).notify(trx, trx.getUserId());
    verify(paymentErrorNotifierServiceMock).notifyCancelPayment(
        any(), anyString(), eq(true), any(InternalServerErrorException.class));
  }

  @Test
  void testSendCancelledTransactionNotification_NotifyThrowsException_ErrorNotifierSucceeds() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");

    RuntimeException exception = new RuntimeException("Notification error");
    when(notifierServiceMock.notify(trx, trx.getUserId())).thenThrow(exception);
    when(notifierServiceMock.buildMessage(trx, trx.getUserId())).thenReturn(Mockito.mock(org.springframework.messaging.Message.class));
    when(paymentErrorNotifierServiceMock.notifyCancelPayment(
        any(), anyString(), eq(true), eq(exception)))
        .thenReturn(true);

    ReflectionTestUtils.invokeMethod(service, "sendCancelledTransactionNotification", trx, true);

    verify(notifierServiceMock).notify(trx, trx.getUserId());
    verify(paymentErrorNotifierServiceMock).notifyCancelPayment(
        any(), anyString(), eq(true), eq(exception));
  }

  @Test
  void testSendCancelledTransactionNotification_NotifyThrowsException_ErrorNotifierFails() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");

    RuntimeException exception = new RuntimeException("Notification error");
    when(notifierServiceMock.notify(trx, trx.getUserId())).thenThrow(exception);
    when(notifierServiceMock.buildMessage(trx, trx.getUserId())).thenReturn(Mockito.mock(org.springframework.messaging.Message.class));
    when(paymentErrorNotifierServiceMock.notifyCancelPayment(
        any(), anyString(), eq(true), eq(exception)))
        .thenReturn(false);

    ReflectionTestUtils.invokeMethod(service, "sendCancelledTransactionNotification", trx, false);

    verify(notifierServiceMock).notify(trx, trx.getUserId());
    verify(paymentErrorNotifierServiceMock).notifyCancelPayment(
        any(), anyString(), eq(true), eq(exception));
  }

  @ParameterizedTest
  @CsvSource({
      "true",
      "false"
  })
  void testSendCancelledTransactionNotification_WithBothResetValues(boolean isReset) {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setMerchantId("MERCHID");
    trx.setAcquirerId("ACQID");

    when(notifierServiceMock.notify(trx, trx.getUserId())).thenReturn(true);

    ReflectionTestUtils.invokeMethod(service, "sendCancelledTransactionNotification", trx, isReset);

    verify(notifierServiceMock).notify(trx, trx.getUserId());
    verifyNoInteractions(paymentErrorNotifierServiceMock);
  }
}


