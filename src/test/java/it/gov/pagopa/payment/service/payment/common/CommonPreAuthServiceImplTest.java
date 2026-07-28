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
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonPreAuthServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnector;
    @Mock
    private AuditUtilities auditUtilities;
    @Mock
    private WalletConnector walletConnector;

    private CommonPreAuthServiceImpl commonPreAuthService;

    private static final long EXPIRATION_MINUTES = 15;
    private static final String INITIATIVE_ID = "INITIATIVE_1";
    private static final String USER_ID = "USER_1";
    private static final String TRX_CODE = "TRX_CODE_1";
    private static final String TRX_ID = "TRX_ID_1";
    private static final String CHANNEL = "APP";

    @BeforeEach
    void setUp() {
        commonPreAuthService = new CommonPreAuthServiceImpl(
                EXPIRATION_MINUTES,
                transactionRepository,
                rewardCalculatorConnector,
                auditUtilities,
                walletConnector
        );
    }

    // =========================================================================
    // 1. TEST RELATE USER & CHECK PRE AUTH
    // =========================================================================

    @Test
    @DisplayName("relateUser - Successo")
    void testRelateUser_Success() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");

        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        Transaction result = commonPreAuthService.relateUser(trx, USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
    }

    @Test
    @DisplayName("relateUser - Fallimento con ServiceException e log di audit dell'errore")
    void testRelateUser_ServiceExceptionHandled() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_SUSPENDED);

        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserSuspendedException.class,
                () -> commonPreAuthService.relateUser(trx, USER_ID));

        verify(auditUtilities).logErrorRelatedUserToTransaction(TRX_CODE, USER_ID);
    }

    @Test
    @DisplayName("checkPreAuth - Wallet in stato SUSPENDED")
    void testCheckPreAuth_WalletSuspended() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_SUSPENDED);

        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserSuspendedException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    @Test
    @DisplayName("checkPreAuth - Wallet in stato UNSUBSCRIBED")
    void testCheckPreAuth_WalletUnsubscribed() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus(PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);

        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserNotOnboardedException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    @Test
    @DisplayName("checkPreAuth - Transazione Scaduta")
    void testCheckPreAuth_TransactionExpired() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")).minusMinutes(EXPIRATION_MINUTES + 1));

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    @Test
    @DisplayName("checkPreAuth - Transazione assegnata a un altro utente")
    void testCheckPreAuth_AssignedToAnotherUser() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);
        trx.setUserId("OTHER_USER");

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(UserNotAllowedException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    @Test
    @DisplayName("checkPreAuth - Transazione già in stato AUTHORIZED")
    void testCheckPreAuth_AlreadyAuthorized() {
        Transaction trx = createTransaction(SyncTrxStatus.AUTHORIZED);

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(TransactionAlreadyAuthorizedException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    @Test
    @DisplayName("checkPreAuth - Stato transazione non consentito (es. REJECTED)")
    void testCheckPreAuth_OperationNotAllowed() {
        Transaction trx = createTransaction(SyncTrxStatus.REJECTED);

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setStatus("ACTIVE");
        when(walletConnector.getWallet(INITIATIVE_ID, USER_ID)).thenReturn(walletDTO);

        assertThrows(OperationNotAllowedException.class,
                () -> commonPreAuthService.checkPreAuth(USER_ID, trx));
    }

    // =========================================================================
    // 2. TEST PREVIEW PAYMENT
    // =========================================================================

    @Test
    @DisplayName("previewPayment - Successo con calcolo del budget residuo")
    void testPreviewPayment_Success() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        AuthPaymentDTO previewDTO = new AuthPaymentDTO();
        previewDTO.setStatus(SyncTrxStatus.IDENTIFIED);
        previewDTO.setRewardCents(100L);

        Reward reward = new Reward();
        RewardCounters counters = new RewardCounters();
        counters.setInitiativeBudgetCents(1000L);
        counters.setExhaustedBudget(false);
        reward.setCounters(counters);

        previewDTO.setRewards(Map.of(INITIATIVE_ID, reward));

        when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(previewDTO);

        AuthPaymentDTO result = commonPreAuthService.previewPayment(trx, CHANNEL, SyncTrxStatus.IDENTIFIED);

        assertNotNull(result);
        assertEquals(SyncTrxStatus.IDENTIFIED, result.getStatus());
        assertEquals(1100L, result.getResidualBudgetCents()); // 1000 residual + 100 reward
        assertEquals(CHANNEL, trx.getChannel());
        assertNotNull(trx.getTrxChargeDate());

        verify(transactionRepository).updateTrxWithStatusForPreview(
                eq(trx),
                eq(previewDTO),
                anyMap(),
                eq(CHANNEL),
                eq(SyncTrxStatus.IDENTIFIED),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("previewPayment - Successo con rewards null (budget residuo null)")
    void testPreviewPayment_Success_NullRewards() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        AuthPaymentDTO previewDTO = new AuthPaymentDTO();
        previewDTO.setStatus(SyncTrxStatus.IDENTIFIED);
        previewDTO.setRewardCents(100L);
        previewDTO.setRewards(Map.of());

        when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(previewDTO);

        AuthPaymentDTO result = commonPreAuthService.previewPayment(trx, CHANNEL, SyncTrxStatus.IDENTIFIED);

        assertNotNull(result);
        assertNull(result.getResidualBudgetCents());
    }

    @Test
    @DisplayName("previewPayment - Rifiutata per Budget Esaurito (BudgetExhaustedException)")
    void testPreviewPayment_Rejected_BudgetExhausted() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        AuthPaymentDTO previewDTO = new AuthPaymentDTO();
        previewDTO.setStatus(SyncTrxStatus.REJECTED);
        previewDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

        when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(previewDTO);

        assertThrows(BudgetExhaustedException.class,
                () -> commonPreAuthService.previewPayment(trx, CHANNEL, SyncTrxStatus.IDENTIFIED));

        verify(transactionRepository).updateTrxRejected(
                eq(TRX_ID),
                eq(USER_ID),
                eq(SyncTrxStatus.REJECTED),
                eq(previewDTO.getRejectionReasons()),
                anyMap(),
                eq(CHANNEL),
                any(OffsetDateTime.class)
        );
        verify(auditUtilities).logErrorPreviewTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL);
    }

    @Test
    @DisplayName("previewPayment - Rifiutata per motivo generico (TransactionRejectedException)")
    void testPreviewPayment_Rejected_Generic() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        AuthPaymentDTO previewDTO = new AuthPaymentDTO();
        previewDTO.setStatus(SyncTrxStatus.REJECTED);
        previewDTO.setRejectionReasons(List.of("GENERIC_REASON"));

        when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(previewDTO);

        assertThrows(TransactionRejectedException.class,
                () -> commonPreAuthService.previewPayment(trx, CHANNEL, SyncTrxStatus.IDENTIFIED));

        verify(auditUtilities).logErrorPreviewTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL);
    }

    // =========================================================================
    // 3. TEST AUDIT LOG RELATE USER
    // =========================================================================

    @Test
    @DisplayName("auditLogRelateUser - Verifica invocazione audit utility")
    void testAuditLogRelateUser() {
        Transaction trx = createTransaction(SyncTrxStatus.CREATED);

        commonPreAuthService.auditLogRelateUser(trx, CHANNEL);

        verify(auditUtilities).logRelatedUserToTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, USER_ID, CHANNEL);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Transaction createTransaction(SyncTrxStatus status) {
        Transaction trx = new Transaction();
        trx.setId(TRX_ID);
        trx.setTrxCode(TRX_CODE);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setUserId(USER_ID);
        trx.setStatus(status);
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        return trx;
    }
}