package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
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
class IdpayCodePreAuthServiceTest {
    private static final String USER_ID = "userId";
    private static final String IDPAYCODE = "IDPAYCODE";
    private static final String FISCALCODE = "FISCALCODE";

    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private EncryptRestConnector encryptRestConnectorMock;

    @Mock private AuditUtilities auditUtilitiesMock;

    private IdpayCodePreAuthService idpayCodePreAuthService;

    private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";
    private static final String USER_ID1 = "USERID1";

    @BeforeEach
    void setUp() {
        long authorizationExpirationMinutes = 4350;
        idpayCodePreAuthService = new IdpayCodePreAuthServiceImpl(
                authorizationExpirationMinutes,
                transactionInProgressRepositoryMock,
                rewardCalculatorConnectorMock,
                auditUtilitiesMock,
                walletConnectorMock,
                encryptRestConnectorMock,
                new RelateUserResponseMapper());
    }

    @Test
    void relateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(encryptRestConnectorMock.upsertToken(Mockito.any()))
                .thenReturn(new EncryptedCfDTO(USER_ID));

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);



        RelateUserResponse result = idpayCodePreAuthService.relateUser(trx.getId(), new RelateUserRequest("FISCAL_CODE"));

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRelateUserIdentified(anyString(), anyString(), any());
        verify(walletConnectorMock, times(1)).getWallet(anyString(), anyString());
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
        IllegalStateException result = Assertions.assertThrows(IllegalStateException.class, () ->
                idpayCodePreAuthService.relateUser(trxId, request)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getMessage());

    }
}