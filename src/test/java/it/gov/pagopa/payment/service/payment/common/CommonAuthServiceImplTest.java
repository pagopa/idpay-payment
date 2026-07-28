package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAuthServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnector;
    @Mock
    private AuditUtilities auditUtilities;
    @Mock
    private WalletConnector walletConnector;
    @Mock
    private CommonPreAuthServiceImpl commonPreAuthService;
    @Mock
    private AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService;

    private CommonAuthServiceImpl commonAuthService;

    private static final String INITIATIVE_ID = "INITIATIVE_1";
    private static final String USER_ID = "USER_1";
    private static final String TRX_CODE = "TRX_CODE_1";
    private static final String TRX_ID = "TRX_ID_1";

    @BeforeEach
    void setUp() {
        commonAuthService = new CommonAuthServiceImpl(
                transactionRepository,
                rewardCalculatorConnector,
                auditUtilities,
                walletConnector,
                commonPreAuthService,
                timeoutSchedulerService
        );
    }

    // =========================================================================
    // 1. TEST PREVIEW PAYMENT
    // =========================================================================

    @Test
    @DisplayName("previewPayment - Successo con userId da transaction")
    void testPreviewPayment_Success() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        trx.setUserId(USER_ID);

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("REFUNDABLE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        AuthPaymentDTO expectedDto = new AuthPaymentDTO();
        when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(expectedDto);

        AuthPaymentDTO result = commonAuthService.previewPayment(trx, null);

        assertNotNull(result);
        assertNotNull(trx.getTrxChargeDate());
        verify(rewardCalculatorConnector).previewTransaction(trx);
    }

    // =========================================================================
    // 2. TEST CHECK WALLET STATUS
    // =========================================================================

    @Test
    @DisplayName("checkWalletStatus - Utente sospeso (UserSuspendedException)")
    void testCheckWalletStatus_Suspended() {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_SUSPENDED);
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserSuspendedException.class,
                () -> commonAuthService.checkWalletStatus(INITIATIVE_ID, USER_ID));
    }

    @Test
    @DisplayName("checkWalletStatus - Utente disiscritto (UserNotOnboardedException)")
    void testCheckWalletStatus_Unsubscribed() {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserNotOnboardedException.class,
                () -> commonAuthService.checkWalletStatus(INITIATIVE_ID, USER_ID));
    }

    @Test
    @DisplayName("checkWalletStatusAndReturn - Successo e restituzione WalletDTO")
    void testCheckWalletStatusAndReturn_Success() {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        WalletDTO result = commonAuthService.checkWalletStatusAndReturn(INITIATIVE_ID, USER_ID);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    @DisplayName("checkWalletStatusAndReturn - Sospeso")
    void testCheckWalletStatusAndReturn_Suspended() {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_SUSPENDED);
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserSuspendedException.class,
                () -> commonAuthService.checkWalletStatusAndReturn(INITIATIVE_ID, USER_ID));
    }

    @Test
    @DisplayName("checkWalletStatusAndReturn - Disiscritto")
    void testCheckWalletStatusAndReturn_Unsubscribed() {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserNotOnboardedException.class,
                () -> commonAuthService.checkWalletStatusAndReturn(INITIATIVE_ID, USER_ID));
    }

    // =========================================================================
    // 3. TEST CHECK AUTH
    // =========================================================================

    @Test
    @DisplayName("checkAuth - Transazione Null (TransactionNotFoundOrExpiredException)")
    void testCheckAuth_TransactionNull() {
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> commonAuthService.checkAuth(TRX_CODE, null));
    }

    @Test
    @DisplayName("checkAuth - Transazione in stato CAPTURED (OperationNotAllowedException)")
    void testCheckAuth_CapturedStatus() {
        Transaction trx = createTransaction(SyncTrxStatus.CAPTURED);
        assertThrows(OperationNotAllowedException.class,
                () -> commonAuthService.checkAuth(TRX_CODE, trx));
    }

    // =========================================================================
    // 4. TEST CHECK TRX STATUS TO INVOKE PRE AUTH
    // =========================================================================

    @Test
    @DisplayName("checkTrxStatusToInvokePreAuth - Stato CREATED con userId")
    void testCheckTrxStatusToInvokePreAuth_CreatedWithUserId() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        trx.setUserId(USER_ID);
        trx.setChannel("APP");

        AuthPaymentDTO preAuth = new AuthPaymentDTO();
        preAuth.setStatus(SyncTrxStatus.AUTHORIZATION_REQUESTED);
        preAuth.setRewardCents(100L);
        preAuth.setRewards(Map.of(INITIATIVE_ID, new Reward()));
        preAuth.setRejectionReasons(Collections.emptyList());
        preAuth.setCounterVersion(1L);

        when(commonPreAuthService.previewPayment(trx, "APP", SyncTrxStatus.AUTHORIZATION_REQUESTED)).thenReturn(preAuth);

        commonAuthService.checkTrxStatusToInvokePreAuth(trx);

        assertEquals(SyncTrxStatus.AUTHORIZATION_REQUESTED, trx.getStatus());
        assertEquals(100L, trx.getRewardCents());
        verify(transactionRepository).updateTrxWithStatus(eq(trx), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("checkTrxStatusToInvokePreAuth - Stato IDENTIFIED con rewardCents null")
    void testCheckTrxStatusToInvokePreAuth_IdentifiedNullReward() {
        Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);
        trx.setRewardCents(null);

        AuthPaymentDTO preAuth = new AuthPaymentDTO();
        preAuth.setStatus(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        when(commonPreAuthService.previewPayment(eq(trx), any(), eq(SyncTrxStatus.AUTHORIZATION_REQUESTED))).thenReturn(preAuth);

        commonAuthService.checkTrxStatusToInvokePreAuth(trx);

        assertEquals(SyncTrxStatus.AUTHORIZATION_REQUESTED, trx.getStatus());
        verify(transactionRepository).updateTrxWithStatus(eq(trx), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("checkTrxStatusToInvokePreAuth - Stato IDENTIFIED con rewardCents valorizzato")
    void testCheckTrxStatusToInvokePreAuth_IdentifiedWithReward() {
        Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);
        trx.setRewardCents(500L);

        commonAuthService.checkTrxStatusToInvokePreAuth(trx);

        assertEquals(SyncTrxStatus.AUTHORIZATION_REQUESTED, trx.getStatus());
        verify(transactionRepository).updateTrxWithStatus(eq(trx), any(LocalDateTime.class));
        verifyNoInteractions(commonPreAuthService);
    }

    // =========================================================================
    // 5. TEST INVOKE RULE ENGINE & UPDATE TRX AUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("invokeRuleEngine - Successo REWARDED e updateTrxAuthorized OK")
    void testInvokeRuleEngine_Rewarded_UpdateAuthorizedSuccess() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REWARDED);
        authDto.setInitiativeId(INITIATIVE_ID);
        authDto.setRejectionReasons(Collections.emptyList());
        RewardCounters counters = new RewardCounters();
        counters.setVersion(2L);
        authDto.setCounters(counters);

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(trx)).thenReturn(authDto);
        when(transactionRepository.updateTrxAuthorized(
                eq(trx), eq(authDto), anyMap(), eq(SyncTrxStatus.AUTHORIZATION_REQUESTED),
                eq(SyncTrxStatus.AUTHORIZED), any(LocalDateTime.class), eq("EUR")
        )).thenReturn(1); // 1 riga aggiornata = Successo

        AuthPaymentDTO result = commonAuthService.invokeRuleEngine(trx);

        assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());
        assertEquals(2L, trx.getCounterVersion());
        verify(timeoutSchedulerService).cancelScheduledMessage(123L);
    }

    @Test
    @DisplayName("invokeRuleEngine - REWARDED ma updateTrxAuthorized fallisce (Timeout/0 righe)")
    void testInvokeRuleEngine_Rewarded_UpdateAuthorizedTimeout() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REWARDED);
        authDto.setInitiativeId(INITIATIVE_ID);
        RewardCounters rewardCounters = new RewardCounters();
        rewardCounters.setVersion(1L);
        rewardCounters.setInitiativeBudgetCents(1000L);
        authDto.setCounters(rewardCounters);

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(trx)).thenReturn(authDto);
        when(transactionRepository.updateTrxAuthorized(
                any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0); // 0 righe aggiornate = Timeout

        AuthPaymentDTO result = commonAuthService.invokeRuleEngine(trx);

        assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
        assertTrue(result.getRejectionReasons().contains(PaymentConstants.PAYMENT_AUTHORIZATION_TIMEOUT));
        assertEquals(0L, result.getCounterVersion());
        assertNull(result.getRewardCents());
        verify(timeoutSchedulerService).cancelScheduledMessage(123L);
    }

    @Test
    @DisplayName("invokeRuleEngine - Rifiutata per Budget Esaurito (BudgetExhaustedException)")
    void testInvokeRuleEngine_Rejected_BudgetExhausted() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REJECTED);
        authDto.setInitiativeId(INITIATIVE_ID);
        authDto.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(trx)).thenReturn(authDto);

        assertThrows(BudgetExhaustedException.class, () -> commonAuthService.invokeRuleEngine(trx));

        verify(transactionRepository).updateTrxRejected(eq(trx), eq(SyncTrxStatus.REJECTED), anyList(), anyMap(), any(LocalDateTime.class), eq("EUR"));
        verify(timeoutSchedulerService).cancelScheduledMessage(123L);
    }

    @Test
    @DisplayName("invokeRuleEngine - Rifiutata per PAYMENT_CANNOT_GUARANTEE_REWARD (Ritorna AuthPaymentDTO)")
    void testInvokeRuleEngine_Rejected_CannotGuaranteeReward() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REJECTED);
        authDto.setInitiativeId(INITIATIVE_ID);
        authDto.setRejectionReasons(List.of(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(trx)).thenReturn(authDto);

        AuthPaymentDTO result = commonAuthService.invokeRuleEngine(trx);

        assertNotNull(result);
        assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
        verify(timeoutSchedulerService).cancelScheduledMessage(123L);
    }

    @Test
    @DisplayName("invokeRuleEngine - Rifiutata generica (TransactionRejectedException)")
    void testInvokeRuleEngine_Rejected_Generic() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REJECTED);
        authDto.setInitiativeId(INITIATIVE_ID);
        authDto.setRejectionReasons(List.of("GENERIC_REASON"));

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(trx)).thenReturn(authDto);

        assertThrows(TransactionRejectedException.class, () -> commonAuthService.invokeRuleEngine(trx));

        verify(timeoutSchedulerService).cancelScheduledMessage(123L);
    }

    @Test
    @DisplayName("invokeRuleEngine - Transazione già autorizzata (TransactionAlreadyAuthorizedException)")
    void testInvokeRuleEngine_AlreadyAuthorized() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);

        assertThrows(TransactionAlreadyAuthorizedException.class,
                () -> commonAuthService.invokeRuleEngine(trx));
    }

    @Test
    @DisplayName("invokeRuleEngine - Stato non gestito (OperationNotAllowedException)")
    void testInvokeRuleEngine_OperationNotAllowed() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        assertThrows(OperationNotAllowedException.class,
                () -> commonAuthService.invokeRuleEngine(trx));
    }

    // =========================================================================
    // 6. TEST AUTH PAYMENT (ORCHESTRAZIONE)
    // =========================================================================

    @Test
    @DisplayName("authPayment - Flusso completo con successo e calcolo residuo budget")
    void testAuthPayment_Success() {
        Transaction trx = createTransaction(SyncTrxStatus.IDENTIFIED);
        trx.setRewardCents(100L);

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        AuthPaymentDTO authDto = new AuthPaymentDTO();
        authDto.setStatus(SyncTrxStatus.REWARDED);
        authDto.setInitiativeId(INITIATIVE_ID);
        authDto.setId(TRX_ID);
        authDto.setRewardCents(50L);
        authDto.setRejectionReasons(Collections.emptyList());
        RewardCounters counters = new RewardCounters();
        counters.setVersion(1L);
        authDto.setCounters(counters);

        Reward reward = new Reward();
        RewardCounters rewardCounters = new RewardCounters();
        rewardCounters.setVersion(1L);
        rewardCounters.setInitiativeBudgetCents(1000L);
        reward.setCounters(rewardCounters);
        authDto.setRewards(Map.of(INITIATIVE_ID, reward));

        when(timeoutSchedulerService.scheduleMessage(TRX_ID)).thenReturn(123L);
        when(rewardCalculatorConnector.authorizePayment(any())).thenReturn(authDto);
        when(transactionRepository.updateTrxAuthorized(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        AuthPaymentDTO result = commonAuthService.authPayment(trx, USER_ID, TRX_CODE);

        assertNotNull(result);
        verify(auditUtilities).logAuthorizedPayment(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, 50L, Collections.emptyList());
    }

    @Test
    @DisplayName("authPayment - Gestione eccezione e logErrorAuthorizedPayment")
    void testAuthPayment_ExceptionThrown() {
        Transaction trx = createTransaction(SyncTrxStatus.CAPTURED); // Provoca OperationNotAllowedException in checkAuth

        assertThrows(OperationNotAllowedException.class,
                () -> commonAuthService.authPayment(trx, USER_ID, TRX_CODE));

        verify(auditUtilities).logErrorAuthorizedPayment(TRX_CODE, USER_ID);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Transaction createTransaction(SyncTrxStatus status) {
        Transaction trx = new Transaction();
        trx.setId(TRX_ID);
        trx.setTrxCode(TRX_CODE);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setStatus(status);
        return trx;
    }
}