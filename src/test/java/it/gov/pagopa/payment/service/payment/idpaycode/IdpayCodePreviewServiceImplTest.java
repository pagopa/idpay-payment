package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.AuthPaymentIdpayCodeDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodePreviewServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private PaymentInstrumentConnector paymentInstrumentConnectorMock;
    @Mock
    private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock
    private AuthPaymentMapper authPaymentMapperMock;
    @Mock
    private AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapperMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @InjectMocks
    private IdpayCodePreviewServiceImpl idpayCodePreviewService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String USER_ID = "USER_ID_123";
    private static final String INITIATIVE_ID = "INITIATIVE_ID_123";
    private static final String SECOND_FACTOR = "SECOND_FACTOR_VAL";


    @Test
    void testPreviewPayment_Success_WithUserId() {
        // Given
        Transaction transaction = createDummyTransaction(USER_ID, MERCHANT_ID);
        SecondFactorDTO secondFactorDTO = new SecondFactorDTO(SECOND_FACTOR);
        AuthPaymentDTO baseAuthPaymentDTO = new AuthPaymentDTO();
        AuthPaymentIdpayCodeDTO mappedAuthPaymentDTO = new AuthPaymentIdpayCodeDTO();

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(paymentInstrumentConnectorMock.getSecondFactor(USER_ID)).thenReturn(secondFactorDTO);
        doNothing().when(commonPreAuthServiceMock).checkPreAuth(USER_ID, transaction);
        when(commonPreAuthServiceMock.previewPayment(transaction, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenReturn(baseAuthPaymentDTO);
        when(authPaymentIdpayCodeMapperMock.authPaymentMapper(baseAuthPaymentDTO, SECOND_FACTOR))
                .thenReturn(mappedAuthPaymentDTO);

        // When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(TRX_ID, MERCHANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(mappedAuthPaymentDTO, result);

        verify(transactionRepositoryMock, times(1)).findById(TRX_ID);
        verify(paymentInstrumentConnectorMock, times(1)).getSecondFactor(USER_ID);
        verify(commonPreAuthServiceMock, times(1)).checkPreAuth(USER_ID, transaction);
        verify(commonPreAuthServiceMock, times(1)).previewPayment(transaction, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED);
        verify(auditUtilitiesMock, times(1)).logPreviewTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        verify(authPaymentIdpayCodeMapperMock, times(1)).authPaymentMapper(baseAuthPaymentDTO, SECOND_FACTOR);
    }

    @Test
    void testPreviewPayment_Success_WithoutUserId() {
        // Given
        Transaction transaction = createDummyTransaction(null, MERCHANT_ID);
        AuthPaymentDTO mappedAuthPaymentDTO = new AuthPaymentDTO();

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(authPaymentMapperMock.transactionMapper(transaction)).thenReturn(mappedAuthPaymentDTO);

        // When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(TRX_ID, MERCHANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(mappedAuthPaymentDTO, result);

        verify(transactionRepositoryMock, times(1)).findById(TRX_ID);
        verify(authPaymentMapperMock, times(1)).transactionMapper(transaction);
        verify(paymentInstrumentConnectorMock, never()).getSecondFactor(any());
        verify(commonPreAuthServiceMock, never()).checkPreAuth(any(), any());
        verify(commonPreAuthServiceMock, never()).previewPayment(any(), any(), any());
        verify(auditUtilitiesMock, never()).logPreviewTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void testPreviewPayment_MerchantMismatch_ThrowsMerchantOrAcquirerNotAllowedException() {
        // Given
        Transaction transaction = createDummyTransaction(USER_ID, "OTHER_MERCHANT_ID");

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        MerchantOrAcquirerNotAllowedException exception = assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> idpayCodePreviewService.previewPayment(TRX_ID, MERCHANT_ID)
        );

        assertEquals(PaymentConstants.ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, exception.getCode());
        assertTrue(exception.getMessage().contains("associated to the transaction is not equal to the merchant"));

        verify(transactionRepositoryMock, times(1)).findById(TRX_ID);
        verify(paymentInstrumentConnectorMock, never()).getSecondFactor(any());
        verify(commonPreAuthServiceMock, never()).checkPreAuth(any(), any());
    }

    @Test
    void testPreviewPayment_TransactionNotFound_ThrowsTransactionNotFoundOrExpiredException() {
        // Given
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> idpayCodePreviewService.previewPayment(TRX_ID, MERCHANT_ID)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with transactionId"));
        verify(transactionRepositoryMock, times(1)).findById(TRX_ID);
    }

    private Transaction createDummyTransaction(String userId, String merchantId) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(TRX_CODE);
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId(userId);
        transaction.setMerchantId(merchantId);
        return transaction;
    }
}