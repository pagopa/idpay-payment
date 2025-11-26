package it.gov.pagopa.payment.service.payment.common;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAuthServiceImplTest {
    private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";
    private static final String USERID = "USERID1";
    private static final String TRX_CODE = "trxcode1";
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock private AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerServiceMock;

    private CommonAuthServiceImpl commonAuthService;
    @BeforeEach
    void setUp() {
        commonAuthService = new CommonAuthServiceImpl(transactionInProgressRepositoryMock,
                rewardCalculatorConnectorMock,
                auditUtilitiesMock,
                walletConnectorMock,
                commonPreAuthServiceMock,
                timeoutSchedulerServiceMock);
    }

    @Test
    void authPayment() {
        TransactionInProgress transaction = getTransactionInProgress();

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);
        authPaymentDTO.setRejectionReasons(Collections.emptyList());

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

        when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

        when(transactionInProgressRepositoryMock.updateTrxAuthorized(transaction, authPaymentDTO,
                CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), List.of())))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

        //When
        AuthPaymentDTO result = commonAuthService.authPayment(transaction, USERID, TRX_CODE);

        //Then
        commonVerifyForAuthPayment(transaction);
        assertEquals(authPaymentDTO, result);
        TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor","splitPayment",
                "residualAmountCents");
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        assertTrue(result.getRejectionReasons().isEmpty());
        assertEquals(Collections.emptyList(), result.getRejectionReasons());
    }

    @Test
    void authPaymentButUpdateTrxFailed() {
        TransactionInProgress transaction = getTransactionInProgress();

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);
        authPaymentDTO.setRejectionReasons(Collections.emptyList());

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);


        when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

        when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

        when(transactionInProgressRepositoryMock.updateTrxAuthorized(transaction, authPaymentDTO,
                CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), List.of())))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

        AuthPaymentDTO result = commonAuthService.authPayment(transaction, transaction.getUserId(), transaction.getTrxCode());

        commonVerifyForAuthPayment(transaction);
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
        assertEquals(List.of(PaymentConstants.PAYMENT_AUTHORIZATION_TIMEOUT), result.getRejectionReasons());
        assertEquals(Collections.emptyMap(), result.getRewards());
        assertNull(result.getRewardCents());
        assertNull(result.getCounters());
    }


    @Test
    void authPaymentWhenRejected() {
        TransactionInProgress transaction = commonAuthPaymentWhenRejectedGiven("DUMMYREJECTIONREASON");

        TransactionRejectedException result =
                assertThrows(TransactionRejectedException.class, () -> commonAuthService.authPayment(transaction, USERID, TRX_CODE));

        commonVerifyForAuthPayment(transaction);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());
    }

    private void commonVerifyForAuthPayment(TransactionInProgress transaction) {
        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);
        verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
        verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    }

    @Test
    void authPaymentWhenRejectedNoBudget() {
        TransactionInProgress transaction = commonAuthPaymentWhenRejectedGiven(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED);

        BudgetExhaustedException result =
                assertThrows(BudgetExhaustedException.class, () -> commonAuthService.authPayment(transaction, USERID, TRX_CODE));

        commonVerifyForAuthPayment(transaction);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());
    }

    @Test
    void authPaymentWhenMismatchVersionCounter() {
        TransactionInProgress transaction = commonAuthPaymentWhenRejectedGiven(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD);

        AuthPaymentDTO result = commonAuthService.authPayment(transaction, USERID, TRX_CODE);

        commonVerifyForAuthPayment(transaction);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
        Assertions.assertTrue(result.getRejectionReasons().contains(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));
    }

    private TransactionInProgress commonAuthPaymentWhenRejectedGiven(String DUMMYREJECTIONREASON) {
        TransactionInProgress transaction = getTransactionInProgress();

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
        authPaymentDTO.setRejectionReasons(List.of(DUMMYREJECTIONREASON));

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);


        when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

        when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

        Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), authPaymentDTO.getRejectionReasons());

        Mockito.doAnswer(
                        invocationOnMock -> {
                            transaction.setStatus(authPaymentDTO.getStatus());
                            transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                            transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
                            return transaction;
                        })
                .when(transactionInProgressRepositoryMock)
                .updateTrxRejected(transaction, authPaymentDTO.getRejectionReasons(), initiativeRejectionReasons);

        doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);
        return transaction;
    }

    @Test
    void authPaymentWhenRewardCalculatorReturn404() {
        TransactionInProgress transaction = getTransactionInProgress();


        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

        when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenThrow(new TransactionNotFoundOrExpiredException("Resource not found on reward-calculator"));

        assertThrows(TransactionNotFoundOrExpiredException.class, () -> commonAuthService.authPayment(transaction, USERID, TRX_CODE));

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);
        verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
        verify(timeoutSchedulerServiceMock, times(0)).cancelScheduledMessage(1L);
    }

    @Test
    void authPaymentAuthorized() {
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction.setUserId("USERID%d".formatted(1));
        transaction.setRewardCents(10L);
        transaction.setRejectionReasons(Collections.emptyList());

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        TransactionAlreadyAuthorizedException result =
                assertThrows(TransactionAlreadyAuthorizedException.class, () -> commonAuthService.authPayment(transaction, USERID, TRX_CODE));

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, result.getCode());
    }

    @Test
    void previewPayment_ok(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "wallet-status");
        when(walletConnectorMock.getWallet(any(),any())).thenReturn(walletDTO);
        when(rewardCalculatorConnectorMock.previewTransaction(any())).thenReturn(authPaymentDTO);
        assertNotNull(commonAuthService.previewPayment(transaction, USERID));
    }

    @Test
    void previewPayment_ko_walletSuspended(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "SUSPENDED");

        when(walletConnectorMock.getWallet(any(),any())).thenReturn(walletDTO);

        UserSuspendedException result =
                assertThrows(UserSuspendedException.class, () -> commonAuthService.previewPayment(transaction, USERID));

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);

        Assertions.assertEquals("PAYMENT_USER_SUSPENDED", result.getCode());
    }


    @Test
    void previewPayment_ko_walletUnsubscribed(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "UNSUBSCRIBED");

        when(walletConnectorMock.getWallet(any(),any())).thenReturn(walletDTO);

        UserNotOnboardedException result =
                assertThrows(UserNotOnboardedException.class, () -> commonAuthService.previewPayment(transaction, USERID));

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);

        Assertions.assertEquals("PAYMENT_USER_UNSUBSCRIBED", result.getCode());
    }

    @Test
    void authPaymentStatusKo() {
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setUserId(USERID);
        transaction.setTrxCode(TRX_CODE);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,transaction);
        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        when(commonPreAuthServiceMock.previewPayment(transaction,transaction.getChannel(),SyncTrxStatus.AUTHORIZATION_REQUESTED)).thenReturn(authPaymentDTO);

        OperationNotAllowedException result =
                assertThrows(OperationNotAllowedException.class, () -> commonAuthService.authPayment(transaction, USERID, TRX_CODE));

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);

        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED, result.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDED", "UNSUBSCRIBED"})
    void authPayment_walletStatusSuspended(String walletStatus) {
        TransactionInProgress transaction = getTransactionInProgress();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, walletStatus);

        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        try {
            commonAuthService.authPayment(transaction, USERID, TRX_CODE);
            Assertions.fail("Expected exception");
        } catch (UserSuspendedException | UserNotOnboardedException e) {
            if(PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
                Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, e.getCode());
            } else {
                Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, e.getCode());
            }
        }

        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USERID);
    }

    @Test
    void authPayment_transactionNotFoundOrExpired(){
        //When
        TransactionNotFoundOrExpiredException resultException = assertThrows(TransactionNotFoundOrExpiredException.class, () -> commonAuthService.authPayment(null, USERID, TRX_CODE));

        //Then
        assertNotNull(resultException);
        assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, resultException.getCode());
        assertEquals("Cannot find transaction with trxCode [%s]".formatted(TRX_CODE), resultException.getMessage());
    }

    @Test
    void checkWalletStatusAndReturn_ok() {
        when(walletConnectorMock.getWallet("INITIATIVE1", "USER1"))
            .thenReturn(walletWithStatus("REFUNDABLE"));

        WalletDTO result = commonAuthService.checkWalletStatusAndReturn("INITIATIVE1", "USER1");

        assertNotNull(result);
        assertEquals("REFUNDABLE", result.getStatus());
        verify(walletConnectorMock, times(1)).getWallet("INITIATIVE1", "USER1");
        verifyNoInteractions(auditUtilitiesMock, rewardCalculatorConnectorMock, transactionInProgressRepositoryMock);
    }

    @Test
    void checkWalletStatusAndReturn_suspended() {
        when(walletConnectorMock.getWallet("INITIATIVE1", "USER1"))
            .thenReturn(walletWithStatus("SUSPENDED"));

        UserSuspendedException ex = assertThrows(UserSuspendedException.class,
            () -> commonAuthService.checkWalletStatusAndReturn("INITIATIVE1", "USER1"));

        assertTrue(ex.getMessage().contains("INITIATIVE1"));
        verify(walletConnectorMock, times(1)).getWallet("INITIATIVE1", "USER1");
    }

    @Test
    void checkWalletStatusAndReturn_unsubscribed() {
        when(walletConnectorMock.getWallet("INITIATIVE1", "USER1"))
            .thenReturn(walletWithStatus("UNSUBSCRIBED"));

        UserNotOnboardedException ex = assertThrows(UserNotOnboardedException.class,
            () -> commonAuthService.checkWalletStatusAndReturn("INITIATIVE1", "USER1"));

        assertTrue(ex.getMessage().contains("INITIATIVE1"));
        verify(walletConnectorMock, times(1)).getWallet("INITIATIVE1", "USER1");
    }

    private WalletDTO walletWithStatus(String status) {
        WalletDTO w = new WalletDTO();
        w.setStatus(status);
        return w;
    }

    private static TransactionInProgress getTransactionInProgress() {
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId(USERID);
        transaction.setTrxCode(TRX_CODE);
        return transaction;
    }
}
