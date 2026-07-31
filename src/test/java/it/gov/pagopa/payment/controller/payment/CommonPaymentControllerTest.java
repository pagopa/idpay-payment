package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.*;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonPaymentControllerImplTest {

  @Mock
  private CommonCreationServiceImpl commonCreationServiceMock;
  @Mock
  private CommonConfirmServiceImpl commonConfirmServiceMock;
  @Mock
  private CommonCancelServiceImpl commonCancelServiceMock;
  @Mock
  private CommonReversalServiceImpl commonReversalServiceMock;
  @Mock
  private CommonInvoiceServiceImpl commonInvoiceServiceMock;
  @Mock
  private CommonStatusTransactionServiceImpl commonStatusTransactionServiceMock;
  @Mock
  private QRCodeExpirationService qrCodeExpirationServiceMock;
  @Mock
  private MultipartFile multipartFileMock;
  @InjectMocks
  private CommonPaymentControllerImpl commonPaymentController;

  @Test
  void testCreateTransaction() {
    // Given
    TransactionCreationRequest request = new TransactionCreationRequest();
    String merchantId = "MERCHANT_ID";
    String acquirerId = "ACQUIRER_ID";
    String idTrxIssuer = "ISSUER_ID";
    TransactionResponse expectedResponse = new TransactionResponse();

    when(commonCreationServiceMock.createTransaction(request, null, merchantId, acquirerId, idTrxIssuer))
            .thenReturn(expectedResponse);

    // When
    TransactionResponse result = commonPaymentController.createTransaction(request, merchantId, acquirerId, idTrxIssuer);

    // Then
    assertNotNull(result);
    assertEquals(expectedResponse, result);
    verify(commonCreationServiceMock, times(1))
            .createTransaction(request, null, merchantId, acquirerId, idTrxIssuer);
  }

  @Test
  void testConfirmPayment() {
    // Given
    String trxId = "TRX_ID";
    String merchantId = "MERCHANT_ID";
    String acquirerId = "ACQUIRER_ID";
    TransactionResponse expectedResponse = new TransactionResponse();

    when(commonConfirmServiceMock.confirmPayment(trxId, merchantId, acquirerId))
            .thenReturn(expectedResponse);

    // When
    TransactionResponse result = commonPaymentController.confirmPayment(trxId, merchantId, acquirerId);

    // Then
    assertNotNull(result);
    assertEquals(expectedResponse, result);
    verify(commonConfirmServiceMock, times(1)).confirmPayment(trxId, merchantId, acquirerId);
  }

  @Test
  void testCancelTransaction() {
    // Given
    String initiativeId = "INITIATIVE_ID";
    String trxId = "TRX_ID";
    String merchantId = "MERCHANT_ID";
    String acquirerId = "ACQUIRER_ID";
    String pointOfSaleId = "POS_ID";

    doNothing().when(commonCancelServiceMock).cancelTransaction(initiativeId, trxId, merchantId, acquirerId, pointOfSaleId);

    // When
    commonPaymentController.cancelTransaction(initiativeId, trxId, merchantId, acquirerId, pointOfSaleId);

    // Then
    verify(commonCancelServiceMock, times(1)).cancelTransaction(initiativeId, trxId, merchantId, acquirerId, pointOfSaleId);
  }

  @Test
  void testReversalTransaction() {
    // Given
    String transactionId = "TRX_ID";
    String merchantId = "MERCHANT_ID";
    String docNumber = "DOC_123";

    doNothing().when(commonReversalServiceMock)
            .reversalTransaction(transactionId, merchantId, multipartFileMock, docNumber);

    // When
    commonPaymentController.reversalTransaction(transactionId, merchantId, multipartFileMock, docNumber);

    // Then
    verify(commonReversalServiceMock, times(1))
            .reversalTransaction(transactionId, merchantId, multipartFileMock, docNumber);
  }

  @Test
  void testInvoiceTransaction() {
    // Given
    String transactionId = "TRX_ID";
    String merchantId = "MERCHANT_ID";
    String docNumber = "DOC_123";

    doNothing().when(commonInvoiceServiceMock)
            .invoiceTransaction(transactionId, merchantId, multipartFileMock, docNumber);

    // When
    commonPaymentController.invoiceTransaction(transactionId, merchantId, multipartFileMock, docNumber);

    // Then
    verify(commonInvoiceServiceMock, times(1))
            .invoiceTransaction(transactionId, merchantId, multipartFileMock, docNumber);
  }

  @Test
  void testCancelPendingTransactions() {
    // Given
    doNothing().when(commonCancelServiceMock).rejectPendingTransactions();

    // When
    commonPaymentController.cancelPendingTransactions();

    // Then
    verify(commonCancelServiceMock, times(1)).rejectPendingTransactions();
  }

  @Test
  void testDeleteLapsedTransaction() {
    // Given
    String initiativeId = "INITIATIVE_ID";
    doNothing().when(commonCancelServiceMock).deleteLapsedTransaction(initiativeId);

    // When
    commonPaymentController.deleteLapsedTransaction(initiativeId);

    // Then
    verify(commonCancelServiceMock, times(1)).deleteLapsedTransaction(initiativeId);
  }

  @Test
  void testGetStatusTransaction() {
    // Given
    String transactionId = "TRX_ID";
    String merchantId = "MERCHANT_ID";
    SyncTrxStatusDTO expectedStatus = new SyncTrxStatusDTO();

    when(commonStatusTransactionServiceMock.getStatusTransaction(transactionId, merchantId))
            .thenReturn(expectedStatus);

    // When
    SyncTrxStatusDTO result = commonPaymentController.getStatusTransaction(transactionId, merchantId);

    // Then
    assertNotNull(result);
    assertEquals(expectedStatus, result);
    verify(commonStatusTransactionServiceMock, times(1)).getStatusTransaction(transactionId, merchantId);
  }

  @Test
  void testForceConfirmTrxExpiration() {
    // Given
    String initiativeId = "INITIATIVE_ID";
    Long expectedCount = 5L;

    when(qrCodeExpirationServiceMock.forceConfirmTrxExpiration(initiativeId))
            .thenReturn(expectedCount);

    // When
    Long result = commonPaymentController.forceConfirmTrxExpiration(initiativeId);

    // Then
    assertEquals(expectedCount, result);
    verify(qrCodeExpirationServiceMock, times(1)).forceConfirmTrxExpiration(initiativeId);
  }

  @Test
  void testForceAuthorizationTrxExpiration() {
    // Given
    String initiativeId = "INITIATIVE_ID";
    Long expectedCount = 3L;

    when(qrCodeExpirationServiceMock.forceAuthorizationTrxExpiration(initiativeId))
            .thenReturn(expectedCount);

    // When
    Long result = commonPaymentController.forceAuthorizationTrxExpiration(initiativeId);

    // Then
    assertEquals(expectedCount, result);
    verify(qrCodeExpirationServiceMock, times(1)).forceAuthorizationTrxExpiration(initiativeId);
  }

  @Test
  void testDeleteInvoicedTransaction() {
    // Given
    doNothing().when(commonCancelServiceMock).deleteInvoicedTransaction();

    // When
    commonPaymentController.deleteInvoicedTransaction();

    // Then
    verify(commonCancelServiceMock, times(1)).deleteInvoicedTransaction();
  }
}