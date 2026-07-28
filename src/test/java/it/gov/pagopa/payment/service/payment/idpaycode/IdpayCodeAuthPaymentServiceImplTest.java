package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodeAuthPaymentServiceImplTest {

    @Mock
    private IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredServiceMock;
    @Mock
    private PaymentInstrumentConnectorImpl paymentInstrumentConnectorMock;
    @Mock
    private CommonAuthServiceImpl commonAuthServiceMock;
    @InjectMocks
    private IdpayCodeAuthPaymentServiceImpl idpayCodeAuthPaymentService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String USER_ID = "USER_ID_123";

    @Test
    void authPayment(){
        PinBlockDTO pinBlockDTO = new PinBlockDTO();
        Transaction transaction = createDummyTransaction(USER_ID);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(TRX_ID))
                .thenReturn(transaction);

        // When
        idpayCodeAuthPaymentService.authPayment(TRX_ID, MERCHANT_ID, pinBlockDTO);

        // Then
        verify(paymentInstrumentConnectorMock).checkPinBlock(pinBlockDTO, USER_ID);
        verify(commonAuthServiceMock).authPayment(transaction, USER_ID, TRX_CODE);
    }

    @Test
    void authPayment_merchantNotFound(){
        PinBlockDTO pinBlockDTO = new PinBlockDTO();
        Transaction transaction = createDummyTransaction(USER_ID);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(TRX_ID))
                .thenReturn(transaction);

        // When & Then
        MerchantOrAcquirerNotAllowedException exception = assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> idpayCodeAuthPaymentService.authPayment(TRX_ID, "DIFFERENT_MERCHANT_ID", pinBlockDTO)
        );

        assertTrue(exception.getMessage().contains("The merchant with id"));
    }

    @Test
    void testAuthPayment_UserNotAssociated_ThrowsOperationNotAllowedException() {
        // Given
        PinBlockDTO pinBlockDTO = new PinBlockDTO();
        Transaction transaction = createDummyTransaction(null);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(TRX_ID))
                .thenReturn(transaction);

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> idpayCodeAuthPaymentService.authPayment(TRX_ID, MERCHANT_ID, pinBlockDTO)
        );

        assertEquals(ExceptionCode.TRX_USER_NOT_ASSOCIATED, exception.getCode());
        assertTrue(exception.getMessage().contains("User not associated to transaction with transactionId"));

        verify(paymentInstrumentConnectorMock, never()).checkPinBlock(any(), any());
        verify(commonAuthServiceMock, never()).authPayment(any(), any(), any());
    }


    @Test
    void testAuthPayment_PinBlockCheckFails_ThrowsException() {
        // Given
        PinBlockDTO pinBlockDTO = new PinBlockDTO();
        Transaction transaction = createDummyTransaction(USER_ID);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(TRX_ID))
                .thenReturn(transaction);
        doThrow(new RuntimeException("Invalid PIN"))
                .when(paymentInstrumentConnectorMock).checkPinBlock(pinBlockDTO, USER_ID);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> idpayCodeAuthPaymentService.authPayment(TRX_ID, MERCHANT_ID, pinBlockDTO)
        );

        assertEquals("Invalid PIN", exception.getMessage());
        verify(commonAuthServiceMock, never()).authPayment(any(), any(), any());
    }

    private Transaction createDummyTransaction(String userId) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(TRX_CODE);
        transaction.setUserId(userId);
        transaction.setMerchantId(IdpayCodeAuthPaymentServiceImplTest.MERCHANT_ID);
        transaction.setStatus(SyncTrxStatus.IDENTIFIED);
        return transaction;
    }
}