package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodeRelateUserServiceImplTest {
    private static final String USER_ID = "userId";
    private static final String FISCALCODE = "FISCALCODE";
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock private EncryptRestConnector encryptRestConnectorMock;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSynchronizer transactionSynchronizer;

    private IdpayCodeRelateUserService idpayCodeRelateUserService;
    @BeforeEach
    void setUp() {
        idpayCodeRelateUserService = new IdpayCodeRelateUserServiceImpl(
                transactionRepository,
                transactionInProgressRepositoryMock,
                commonPreAuthServiceMock,
                encryptRestConnectorMock,
                new RelateUserResponseMapper(),
                transactionSynchronizer);
    }

    @Test
    void relateUser() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenReturn(new EncryptedCfDTO(USER_ID));

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        when(commonPreAuthServiceMock.relateUser(trx, USER_ID)).thenReturn(trx);

        RelateUserResponse result = idpayCodeRelateUserService.relateUser(trx.getId(), FISCALCODE);

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRelateUserIdentified(anyString(), anyString(), any());
    }

    @Test
    void relateUserTrxNotFound() {
        //Given
        String trxId = "trxId";

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenReturn(new EncryptedCfDTO(USER_ID));

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());


        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodeRelateUserService.relateUser(trxId, FISCALCODE)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }
}