package it.gov.pagopa.payment.service.payment.idpaycode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionRejectedException;
import it.gov.pagopa.payment.exception.custom.UserSuspendedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdpayCodePreAuthServiceTest {
    private static final String USER_ID = "userId";
    private static final String FISCALCODE = "FISCALCODE";
    private static final String MERCHANTID = "MERCHANTID";

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
                paymentInstrumentConnectorMock,
                new AuthPaymentIdpayCodeMapper());
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



        RelateUserResponse result = idpayCodePreAuthService.relateUser(trx.getId(), FISCALCODE);

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


        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodePreAuthService.relateUser(trxId, FISCALCODE)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }

    @Test
    void previewPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
        when(rewardCalculatorConnectorMock.previewTransaction(trx))
                .thenReturn(authPaymentDTO);

        doNothing().when(transactionInProgressRepositoryMock)
                .updateTrxWithStatus(trx.getId(),
                        trx.getUserId(),
                        authPaymentDTO.getReward(),
                        Collections.emptyList(),
                        Collections.emptyMap(),
                        authPaymentDTO.getRewards(),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE,
                        SyncTrxStatus.IDENTIFIED,
                        authPaymentDTO.getCounterVersion());

        when(paymentInstrumentConnectorMock.getSecondFactor(trx.getUserId()))
                .thenReturn(new SecondFactorDTO(SECOND_FACTOR));
        //When
        AuthPaymentDTO result = idpayCodePreAuthService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons","splitPayment","residualAmountCents");

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxWithStatus(anyString(), anyString(), anyLong(), eq(Collections.emptyList()), anyMap(), anyMap(),anyString(), any(),anyLong());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(),anyString(), anyList(), anyMap(), anyString());
    }

    @Test
    void previewPaymentNotFound() {
        //Given
        String trxId = "trxId";

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());

        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }

    @Test
    void previewPayment_notRelateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setMerchantId(MERCHANTID);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        AuthPaymentDTO result = idpayCodePreAuthService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result,
                "reward",
                "counters",
                "residualBudget",
                "secondFactor",
                "splitPayment",
                "residualAmountCents"
        );

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_RejectedStatusRE() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

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
                        Map.of(authPaymentDTO.getInitiativeId(), authPaymentDTO.getRejectionReasons()),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE);
        //When
        TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatus(anyString(), anyString(), anyLong(), eq(null), anyMap(), anyMap(),anyString(),any(),anyLong());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(),anyString(), anyList(), anyMap(), anyString());
    }

    @Test
    void previewPayment_BudgetExhaustedRejectedStatusRE() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

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
                        Map.of(authPaymentDTO.getInitiativeId(), authPaymentDTO.getRejectionReasons()),
                        RewardConstants.TRX_CHANNEL_IDPAYCODE);
        //When
        BudgetExhaustedException result = Assertions.assertThrows(BudgetExhaustedException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(1)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatus(anyString(), anyString(), anyLong(), eq(null), anyMap(), anyMap(),anyString(),any(),anyLong());
        verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(),anyString(), anyList(), anyMap(), anyString());
    }

    @Test
    void previewPayment_walletStatusSuspended() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,PaymentConstants.WALLET_STATUS_SUSPENDED);
        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        //When
        UserSuspendedException result = Assertions.assertThrows(UserSuspendedException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(0)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatus(anyString(), anyString(), anyLong(), eq(null), anyMap(), anyMap(), anyString(),any(),anyLong());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(),anyString(), anyList(), anyMap(),anyString());
    }

    @Test
    void previewPayment_differentMerchantId() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

        String trxId = trx.getId();
        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        MerchantOrAcquirerNotAllowedException result = Assertions.assertThrows(MerchantOrAcquirerNotAllowedException.class, () ->
                idpayCodePreAuthService.previewPayment(trxId, "DUMMYMERCHANT")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(rewardCalculatorConnectorMock, times(0)).previewTransaction(any());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatus(anyString(), anyString(), anyLong(), eq(null), anyMap(), anyMap(), anyString(),any(),anyLong());
        verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(),anyString(), anyList(), anyMap(), anyString());
    }
}