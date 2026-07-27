package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CancelTransactionAuditDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonCancelServiceImplTest {

  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private RewardCalculatorConnector rewardCalculatorConnector;
  @Mock
  private TransactionNotifierService notifierService;
  @Mock
  private PaymentErrorNotifierService paymentErrorNotifierService;
  @Mock
  private AuditUtilities auditUtilities;
  @Mock
  private BarCodeCreationServiceImpl barCodeCreationService;
  @InjectMocks
  private CommonCancelServiceImpl commonCancelService;

  private static final String TRX_ID = "TRX_123";
  private static final String MERCHANT_ID = "MERCHANT_1";
  private static final String ACQUIRER_ID = "ACQUIRER_1";
  private static final String POS_ID = "POS_1";
  private static final String INITIATIVE_ID = "INITIATIVE_1";
  private static final String USER_ID = "USER_1";


  // =========================================================================
  // 1. CANCEL TRANSACTION TESTS
  // =========================================================================

  @Test
  @DisplayName("cancelTransaction - Transazione non trovata (TransactionNotFoundOrExpiredException)")
  void testCancelTransaction_NotFound() {
    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.empty());

    assertThrows(TransactionNotFoundOrExpiredException.class,
            () -> commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID));

    verify(auditUtilities).logErrorCancelTransaction(TRX_ID, MERCHANT_ID);
  }

  @Test
  @DisplayName("cancelTransaction - Merchant o Acquirer errati (MerchantOrAcquirerNotAllowedException)")
  void testCancelTransaction_MerchantMismatch() {
    Transaction trx = createTransaction(SyncTrxStatus.CREATED);
    trx.setMerchantId("WRONG_MERCHANT");

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));

    assertThrows(MerchantOrAcquirerNotAllowedException.class,
            () -> commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID));

    verify(auditUtilities).logErrorCancelTransaction(TRX_ID, MERCHANT_ID);
  }

  @Test
  @DisplayName("cancelTransaction - Cancellazione immediata (Stato CREATED)")
  void testCancelTransaction_ImmediatelyDeletable_Created() {
    Transaction trx = createTransaction(SyncTrxStatus.CREATED);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    assertEquals(SyncTrxStatus.CANCELLED, trx.getStatus());
    verify(transactionRepository).save(trx);
    verify(auditUtilities).logCancelTransaction(any(CancelTransactionAuditDTO.class));
  }

  @Test
  @DisplayName("cancelTransaction - Cancellazione immediata (Stato IDENTIFIED)")
  void testCancelTransaction_ImmediatelyDeletable_Identified() {
    Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    assertEquals(SyncTrxStatus.CANCELLED, trx.getStatus());
    verify(transactionRepository).save(trx);
  }

  @Test
  @DisplayName("cancelTransaction - Cancellazione immediata (Stato INVOICED)")
  void testCancelTransaction_ImmediatelyDeletable_Invoiced() {
    Transaction trx = createTransaction(SyncTrxStatus.INVOICED);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    assertEquals(SyncTrxStatus.INVOICED, trx.getStatus());
  }

  @Test
  @DisplayName("cancelTransaction - Stato non cancellabile (OperationNotAllowedException)")
  void testCancelTransaction_OperationNotAllowed() {
    Transaction trx = createTransaction(SyncTrxStatus.REJECTED);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));

    assertThrows(OperationNotAllowedException.class,
            () -> commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID));

    verify(auditUtilities).logErrorCancelTransaction(TRX_ID, MERCHANT_ID);
  }

  @Test
  @DisplayName("cancelTransaction - AUTHORIZED senza extended authorization e refund con successo")
  void testCancelTransaction_Authorized_Success() {
    Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);
    trx.setExtendedAuthorization(false);

    AuthPaymentDTO refund = new AuthPaymentDTO();
    refund.setRewardCents(100L);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnector.cancelTransaction(trx)).thenReturn(refund);
    when(notifierService.notify(trx, USER_ID)).thenReturn(true);

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    assertEquals(SyncTrxStatus.CANCELLED, trx.getStatus());
    assertEquals(100L, trx.getRewardCents());
    assertNotNull(trx.getElaborationDateTime());
    verify(transactionRepository).save(trx);
    verify(notifierService).notify(trx, USER_ID);
    verify(auditUtilities).logCancelTransaction(any(CancelTransactionAuditDTO.class));
  }

  @Test
  @DisplayName("cancelTransaction - AUTHORIZED con extended authorization (Reset e ricreazione trx)")
  void testCancelTransaction_Authorized_ExtendedAuthorization() {
    Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);
    trx.setExtendedAuthorization(true);
    trx.setVoucherAmountCents(500L);
    trx.setChannel("CHANNEL_1");
    trx.setTrxEndDate(LocalDateTime.now(ZoneId.of("Europe/Rome")));

    AuthPaymentDTO refund = new AuthPaymentDTO();
    refund.setRewardCents(500L);

    Transaction newTrx = createTransaction(SyncTrxStatus.CREATED);
    newTrx.setId("NEW_TRX_ID");

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnector.cancelTransaction(trx)).thenReturn(refund);
    when(barCodeCreationService.createExtendedTransactionPostDelete(any(TransactionBarCodeCreationRequest.class), anyString(), anyString(), any()))
            .thenReturn(newTrx);
    when(notifierService.notify(trx, USER_ID)).thenReturn(true);

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    verify(barCodeCreationService).createExtendedTransactionPostDelete(any(), eq("CHANNEL_1"), eq(USER_ID), any());
    verify(transactionRepository, times(2)).save(any(Transaction.class));
    verify(notifierService).notify(trx, USER_ID);
  }

  @Test
  @DisplayName("cancelTransaction - AUTHORIZED e notifica fallisce lanciando eccezione gestita da paymentErrorNotifierService")
  void testCancelTransaction_Authorized_NotificationErrorHandled() {
    Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);
    AuthPaymentDTO refund = new AuthPaymentDTO();

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnector.cancelTransaction(trx)).thenReturn(refund);
    when(notifierService.notify(trx, USER_ID)).thenReturn(false); // Notifica fallisce
    when(paymentErrorNotifierService.notifyCancelPayment(any(), anyString(), eq(true), any())).thenReturn(true);

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    verify(paymentErrorNotifierService).notifyCancelPayment(any(), anyString(), eq(true), any(InternalServerErrorException.class));
  }

  @Test
  @DisplayName("cancelTransaction - AUTHORIZED e eccezione in notifica che fallisce anche nel fallback")
  void testCancelTransaction_Authorized_NotificationErrorFallbackFails() {
    Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);
    AuthPaymentDTO refund = new AuthPaymentDTO();

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnector.cancelTransaction(trx)).thenReturn(refund);
    when(notifierService.notify(trx, USER_ID)).thenThrow(new RuntimeException("Kafka error"));
    when(paymentErrorNotifierService.notifyCancelPayment(any(), anyString(), eq(true), any())).thenReturn(false);

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    verify(paymentErrorNotifierService).notifyCancelPayment(any(), anyString(), eq(true), any(RuntimeException.class));
  }

  @Test
  @DisplayName("cancelTransaction - AUTHORIZED ma refund ritornato dal connettore è null")
  void testCancelTransaction_Authorized_RefundNull() {
    Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);

    when(transactionRepository.findById(TRX_ID)).thenReturn(Optional.of(trx));
    when(rewardCalculatorConnector.cancelTransaction(trx)).thenReturn(null);

    commonCancelService.cancelTransaction(TRX_ID, MERCHANT_ID, ACQUIRER_ID, POS_ID);

    verify(transactionRepository, never()).save(any());
    verify(notifierService, never()).notify(any(), any());
  }

  // =========================================================================
  // 2. REJECT PENDING TRANSACTIONS TESTS
  // =========================================================================

  @Test
  @DisplayName("rejectPendingTransactions - Processa transazioni in sospeso a blocchi fino a svuotamento")
  void testRejectPendingTransactions() {
    Transaction trx1 = createTransaction(SyncTrxStatus.AUTHORIZED);
    trx1.setId("TRX_1");
    Transaction trx2 = createTransaction(SyncTrxStatus.CREATED);
    trx2.setId("TRX_2");

    when(transactionRepository.findByStatusAndUpdateDateBefore(eq(SyncTrxStatus.AUTHORIZED), any(), any(Pageable.class)))
            .thenReturn(List.of(trx1, trx2))
            .thenReturn(Collections.emptyList());

    when(transactionRepository.findById("TRX_1")).thenReturn(Optional.of(trx1));
    when(transactionRepository.findById("TRX_2")).thenReturn(Optional.of(trx2));
    when(rewardCalculatorConnector.cancelTransaction(trx1)).thenReturn(new AuthPaymentDTO());

    commonCancelService.rejectPendingTransactions();

    verify(transactionRepository, times(2)).findByStatusAndUpdateDateBefore(eq(SyncTrxStatus.AUTHORIZED), any(), any(Pageable.class));
    verify(transactionRepository).findById("TRX_1");
    verify(transactionRepository).findById("TRX_2");
  }

  // =========================================================================
  // 3. DELETE INVOICED TRANSACTION TESTS
  // =========================================================================

  @Test
  @DisplayName("deleteInvoicedTransaction - Processa e cancella le transazioni fatturate a blocchi")
  void testDeleteInvoicedTransaction() {
    Transaction trx1 = createTransaction(SyncTrxStatus.INVOICED);
    trx1.setId("INVOICED_1");
    Transaction trx2 = createTransaction(SyncTrxStatus.INVOICED);
    trx2.setId("INVOICED_2");

    when(transactionRepository.findByStatusOrderByTrxDateAsc(eq(SyncTrxStatus.INVOICED), any(Pageable.class)))
            .thenReturn(List.of(trx1, trx2))
            .thenReturn(Collections.emptyList());

    commonCancelService.deleteInvoicedTransaction();

    verify(transactionRepository).bulkDeleteByIds(List.of("INVOICED_1", "INVOICED_2"));
  }

  // =========================================================================
  // 4. DELETE LAPSED TRANSACTION TESTS
  // =========================================================================

  @Test
  @DisplayName("deleteLapsedTransaction - Processa transazioni scadute con initiativeId non nullo")
  void testDeleteLapsedTransaction_WithInitiativeId() {
    Transaction trx1 = createTransaction(SyncTrxStatus.IDENTIFIED);
    trx1.setId("LAPSED_1");

    Transaction trx2 = createTransaction(SyncTrxStatus.CREATED);
    trx2.setId("LAPSED_2");

    when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(trx1, trx2)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

    commonCancelService.deleteLapsedTransaction(INITIATIVE_ID);

    verify(rewardCalculatorConnector).cancelTransaction(trx1);
    verify(transactionRepository).bulkDeleteByIds(List.of("LAPSED_1", "LAPSED_2"));
    verify(auditUtilities, times(2)).logExpiredTransaction(eq(INITIATIVE_ID), anyString(), any(), any(), anyString());
  }

  @Test
  @DisplayName("deleteLapsedTransaction - Con initiativeId nullo")
  void testDeleteLapsedTransaction_NullInitiativeId() {
    Transaction trx = createTransaction(SyncTrxStatus.CREATED);
    trx.setId("LAPSED_1");

    when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(trx)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

    commonCancelService.deleteLapsedTransaction(null);

    verify(transactionRepository).bulkDeleteByIds(List.of("LAPSED_1"));
  }

  @Test
  @DisplayName("handleExpiredTransactionBulk - Gestione eccezione TransactionNotFoundOrExpiredException")
  void testHandleExpiredTransactionBulk_TransactionNotFoundOrExpiredException() {
    Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);

    when(rewardCalculatorConnector.cancelTransaction(trx))
            .thenThrow(new TransactionNotFoundOrExpiredException("Expired"));

    boolean result = commonCancelService.handleExpiredTransactionBulk(trx);

    assertTrue(result);
  }

  @Test
  @DisplayName("handleExpiredTransactionBulk - Gestione eccezione ServiceException (restituisce false)")
  void testHandleExpiredTransactionBulk_ServiceException() {
    Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);

    when(rewardCalculatorConnector.cancelTransaction(trx))
            .thenThrow(new ServiceException("SERVICE_ERR", null));

    boolean result = commonCancelService.handleExpiredTransactionBulk(trx);

    assertFalse(result);
  }

  @Test
  @DisplayName("deleteLapsedTransaction - Gestione eccezione generica durante il processo singolo")
  void testDeleteLapsedTransaction_ExceptionInProcessSingleTransaction() {
    Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);
    trx.setId("LAPSED_ERR");

    when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(trx)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

    when(rewardCalculatorConnector.cancelTransaction(trx)).thenThrow(new RuntimeException("Generic Error"));

    commonCancelService.deleteLapsedTransaction(INITIATIVE_ID);

    verify(auditUtilities).logErrorExpiredTransaction(eq(INITIATIVE_ID), eq("LAPSED_ERR"), any(), any(), anyString());
    verify(transactionRepository, never()).bulkDeleteByIds(anyList());
  }

  // =========================================================================
  // HELPER METHODS
  // =========================================================================

  private Transaction createTransaction(SyncTrxStatus status) {
    Transaction trx = TransactionFaker.mockInstance(1, status);
    trx.setId(TRX_ID);
    trx.setTrxCode("TRX_CODE_1");
    trx.setInitiativeId(INITIATIVE_ID);
    trx.setMerchantId(MERCHANT_ID);
    trx.setAcquirerId(ACQUIRER_ID);
    trx.setPointOfSaleId(POS_ID);
    trx.setUserId(USER_ID);
    trx.setStatus(status);
    return trx;
  }
}