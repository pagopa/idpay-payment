package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
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
class IdpayCodeRelateUserServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock
    private EncryptRestConnector encryptRestConnectorMock;
    @Mock
    private RelateUserResponseMapper relateUserResponseMapperMock;
    @InjectMocks
    private IdpayCodeRelateUserServiceImpl idpayCodeRelateUserService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String FISCAL_CODE = "ABCDEF90A01H501W";
    private static final String USER_ID = "USER_ID_TOKEN_123";


    @Test
    void testRelateUser_Success() {
        // Given
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO();
        encryptedCfDTO.setToken(USER_ID);

        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setStatus(SyncTrxStatus.CREATED);

        Transaction trxInProgress = new Transaction();
        trxInProgress.setId(TRX_ID);
        trxInProgress.setUserId(USER_ID);
        trxInProgress.setStatus(SyncTrxStatus.IDENTIFIED);

        RelateUserResponse expectedResponse = new RelateUserResponse();

        when(encryptRestConnectorMock.upsertToken(any(CFDTO.class))).thenReturn(encryptedCfDTO);
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(commonPreAuthServiceMock.relateUser(transaction, USER_ID)).thenReturn(trxInProgress);
        when(transactionRepositoryMock.save(transaction)).thenReturn(transaction);
        doNothing().when(commonPreAuthServiceMock).auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        when(relateUserResponseMapperMock.transactionMapper(trxInProgress)).thenReturn(expectedResponse);

        // When
        RelateUserResponse result = idpayCodeRelateUserService.relateUser(TRX_ID, FISCAL_CODE);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals(SyncTrxStatus.IDENTIFIED, transaction.getStatus());

        verify(transactionRepositoryMock, times(1)).findById(TRX_ID);
        verify(commonPreAuthServiceMock, times(1)).relateUser(transaction, USER_ID);
        verify(transactionRepositoryMock, times(1)).save(transaction);
        verify(commonPreAuthServiceMock, times(1)).auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        verify(relateUserResponseMapperMock, times(1)).transactionMapper(trxInProgress);
    }

    @Test
    void testRelateUser_TransactionNotFound_ThrowsException() {
        // Given
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO();
        encryptedCfDTO.setToken(USER_ID);

        when(encryptRestConnectorMock.upsertToken(any(CFDTO.class))).thenReturn(encryptedCfDTO);
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> idpayCodeRelateUserService.relateUser(TRX_ID, FISCAL_CODE)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with transactionId"));

        verify(commonPreAuthServiceMock, never()).relateUser(any(), any());
        verify(transactionRepositoryMock, never()).save(any());
        verify(commonPreAuthServiceMock, never()).auditLogRelateUser(any(), any());
        verify(relateUserResponseMapperMock, never()).transactionMapper(any());
    }
}