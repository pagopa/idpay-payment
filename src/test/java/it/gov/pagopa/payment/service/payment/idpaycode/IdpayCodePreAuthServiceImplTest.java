package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpayCodePreAuthServiceImplTest {
    private static final String USER_ID = "userId";
    private static final String IDPAYCODE = "IDPAYCODE";
    private static final String FISCALCODE = "FISCALCODE";
    @Mock private EncryptRestConnector encryptRestConnectorMock;
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private CommonPreAuthService commonPreAuthServiceMock;

    private IdpayCodePreAuthService idpayCodePreAuthService;

    @BeforeEach
    void setUp() {
        long authorizationExpirationMinutes = 4350;
        idpayCodePreAuthService = new IdpayCodePreAuthServiceImpl(
                encryptRestConnectorMock,
                transactionInProgressRepositoryMock,
                commonPreAuthServiceMock,
                new RelateUserResponseMapper());
    }

    @Test
    void relateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenReturn(new EncryptedCfDTO(USER_ID));

        when(transactionInProgressRepositoryMock.findById(trx.getId()))
                .thenReturn(Optional.of(trx));

        when(commonPreAuthServiceMock.relateUser(trx, USER_ID))
                .thenReturn(trx);

        doNothing().when(transactionInProgressRepositoryMock).updateTrxRelateUserIdentified(trx.getId(), USER_ID,IDPAYCODE);

        //When
        RelateUserResponse result = idpayCodePreAuthService.relateUser(trx.getId(), new RelateUserRequest(FISCALCODE));

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

        Mockito.verify(transactionInProgressRepositoryMock, Mockito.times(1)).findById(Mockito.anyString());
        Mockito.verify(commonPreAuthServiceMock, Mockito.times(1)).relateUser(Mockito.any(), Mockito.anyString());
        Mockito.verify(transactionInProgressRepositoryMock, Mockito.times(1)).updateTrxRelateUserIdentified(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void relateUserTrxNotFound() {
        //Given
        String trxId = "trxId";

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenReturn(new EncryptedCfDTO(USER_ID));

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());

        RelateUserRequest request = new RelateUserRequest(FISCALCODE);
        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodePreAuthService.relateUser(trxId, request)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }

    @Test
    void relateUserEncryptError() {
        //Given
        String trxId = "trxId";

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenThrow(new RuntimeException());


        RelateUserRequest request = new RelateUserRequest(FISCALCODE);
        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodePreAuthService.relateUser(trxId, request)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getHttpStatus());
        Assertions.assertEquals("INTERNAL SERVER ERROR", result.getCode());

    }
}