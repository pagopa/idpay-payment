package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.DetailsDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodePreAuthServiceTest {
    private static final String USER_ID = "userId";
    private static final String FISCALCODE = "FISCALCODE";

    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private EncryptRestConnector encryptRestConnectorMock;

    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private PaymentInstrumentConnector paymentInstrumentConnectorMock;

    private IdpayCodePreAuthService idpayCodePreAuthService;

    private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";
    private static final String SECOND_FACTOR = "SECOND_FACTOR";

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
                new RelateUserResponseMapper(),
                new AuthPaymentMapper(),
                paymentInstrumentConnectorMock
        );
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

    @Test
    void previewPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        when(rewardCalculatorConnectorMock.previewTransaction(trx))
                .thenReturn(authPaymentDTO);

        doNothing().when(transactionInProgressRepositoryMock)
                .updateTrxIdentified(trx.getId(),
                        trx.getUserId(),
                        authPaymentDTO.getReward(),
                        null,
                        authPaymentDTO.getRewards(),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE);

        when(paymentInstrumentConnectorMock.getSecondFactor(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(new DetailsDTO(SECOND_FACTOR));
        //When
        AuthPaymentDTO result = idpayCodePreAuthService.previewPayment(trx.getId(), "acquirerId", "merchantFiscalCode");

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons");

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxIdentified(anyString(), anyString(), anyLong(), eq(null), anyMap(),anyString());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(),anyString(), anyList(), anyString());
    }

    @Test
    void previewPaymentNotFound() {
        //Given
        String trxId = "trxId";

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());

        //When
        IllegalStateException result = Assertions.assertThrows(IllegalStateException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, "acquirerId", "merchantFiscalCode")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getMessage());

    }

    @Test
    void previewPayment_notRelateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        AuthPaymentDTO result = idpayCodePreAuthService.previewPayment(trx.getId(), "acquirerId", "merchantFiscalCode");

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result,
                "reward",
                "counters",
                "residualBudget",
                "secondFactor"
        );

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_RejectedStatusRE() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
        authPaymentDTO.setRejectionReasons(List.of("REJECTED_REASON_DUMMY"));
        when(rewardCalculatorConnectorMock.previewTransaction(trx))
                .thenReturn(authPaymentDTO);

        doNothing().when(transactionInProgressRepositoryMock)
                .updateTrxRejected(trx.getId(),
                        trx.getUserId(),
                        authPaymentDTO.getRejectionReasons(),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE);
        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, "acquirerId", "merchantFiscalCode")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), anyLong(), eq(null), anyMap(),anyString());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(),anyString(), anyList(), anyString());
    }

    @Test
    void previewPayment_BudgetExhaustedRejectedStatusRE() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
        authPaymentDTO.setRejectionReasons(List.of("REJECTED_REASON_DUMMY", RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));
        when(rewardCalculatorConnectorMock.previewTransaction(trx))
                .thenReturn(authPaymentDTO);

        doNothing().when(transactionInProgressRepositoryMock)
                .updateTrxRejected(trx.getId(),
                        trx.getUserId(),
                        authPaymentDTO.getRejectionReasons(),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE);
        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, "acquirerId", "merchantFiscalCode")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), anyLong(), eq(null), anyMap(),anyString());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(),anyString(), anyList(), anyString());
    }

    @Test
    void previewPayment_walletStatusSuspended() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,PaymentConstants.WALLET_STATUS_SUSPENDED);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, "acquirerId", "merchantFiscalCode")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(0)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), anyLong(), eq(null), anyMap(),anyString());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(),anyString(), anyList(), anyString());
    }
}