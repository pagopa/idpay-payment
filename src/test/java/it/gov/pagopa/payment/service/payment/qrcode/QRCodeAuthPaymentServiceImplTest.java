package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceImplTest {

    @Mock
    private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
    @Mock
    private CommonAuthServiceImpl commonAuthServiceMock;
    @InjectMocks
    private QRCodeAuthPaymentServiceImpl qrCodeAuthPaymentService;

    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String LOWER_TRX_CODE = "trx_code_123";
    private static final String USER_ID = "USER_ID_123";


    @Test
    void testAuthPayment_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId("TRX_ID_123");
        transaction.setTrxCode(LOWER_TRX_CODE);
        transaction.setUserId(USER_ID);

        AuthPaymentDTO expectedAuthPaymentDTO = new AuthPaymentDTO();
        expectedAuthPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);

        when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE))
                .thenReturn(transaction);
        when(commonAuthServiceMock.authPayment(transaction, USER_ID, TRX_CODE))
                .thenReturn(expectedAuthPaymentDTO);

        // When
        AuthPaymentDTO result = qrCodeAuthPaymentService.authPayment(USER_ID, TRX_CODE);

        // Then
        assertNotNull(result);
        assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

        verify(qrCodeAuthorizationExpiredServiceMock, times(1)).findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE);
        verify(commonAuthServiceMock, times(1)).authPayment(transaction, USER_ID, TRX_CODE);
    }

    @Test
    void testAuthPayment_Success_UserIdNotYetAssignedInTransaction() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId("TRX_ID_123");
        transaction.setTrxCode(LOWER_TRX_CODE);
        transaction.setUserId(null); // Nessun utente precedentemente associato

        AuthPaymentDTO expectedAuthPaymentDTO = new AuthPaymentDTO();
        expectedAuthPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);

        when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE))
                .thenReturn(transaction);
        when(commonAuthServiceMock.authPayment(transaction, USER_ID, TRX_CODE))
                .thenReturn(expectedAuthPaymentDTO);

        // When
        AuthPaymentDTO result = qrCodeAuthPaymentService.authPayment(USER_ID, TRX_CODE);

        // Then
        assertNotNull(result);
        assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

        verify(qrCodeAuthorizationExpiredServiceMock, times(1)).findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE);
        verify(commonAuthServiceMock, times(1)).authPayment(transaction, USER_ID, TRX_CODE);
    }

    @Test
    void testAuthPayment_AlreadyAssignedToDifferentUser_ThrowsUserNotAllowedException() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId("TRX_ID_123");
        transaction.setTrxCode(LOWER_TRX_CODE);
        transaction.setUserId("OTHER_USER_ID");

        when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE))
                .thenReturn(transaction);

        // When & Then
        UserNotAllowedException exception = assertThrows(
                UserNotAllowedException.class,
                () -> qrCodeAuthPaymentService.authPayment(USER_ID, TRX_CODE)
        );

        assertEquals(ExceptionCode.TRX_ALREADY_ASSIGNED, exception.getCode());
        assertTrue(exception.getMessage().contains("is already assigned to another user"));

        verify(qrCodeAuthorizationExpiredServiceMock, times(1)).findByTrxCodeAndAuthorizationNotExpired(LOWER_TRX_CODE);
        verify(commonAuthServiceMock, never()).authPayment(any(), any(), any());
    }
}