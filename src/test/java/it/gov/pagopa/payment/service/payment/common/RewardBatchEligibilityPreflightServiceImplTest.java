package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.rest.rewardbatch.RewardBatchConnector;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.RewardBatchEligibilityNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardBatchEligibilityPreflightServiceImplTest {

    private static final String TRANSACTION_ID = "transaction-id";
    private static final String INITIATIVE_ID = "initiative-id";
    private static final String MERCHANT_ID = "merchant-id";

    @Mock
    private RewardBatchConnector rewardBatchConnector;

    private AppConfigurationProperties.RewardBatchImpact rewardBatchImpact;
    private RewardBatchEligibilityPreflightServiceImpl preflightService;

    @BeforeEach
    void setUp() {
        rewardBatchImpact = new AppConfigurationProperties.RewardBatchImpact();
        preflightService = new RewardBatchEligibilityPreflightServiceImpl(
                rewardBatchImpact, rewardBatchConnector, JsonMapper.builder().build());
    }

    @Test
    void doesNotCallEligibilityWhenDisabled() {
        preflightService.verifyEligibility(transaction(), MERCHANT_ID, null);

        verify(rewardBatchConnector, never()).findEligibility(MERCHANT_ID, TRANSACTION_ID, null);
    }

    @Test
    void permitsNoMembershipWhenEnabled() {
        enableEligibility();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization("scope")))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization("scope")));
    }

    @Test
    void appliesBasicPolicy() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "IGNORED")));

        assertDoesNotThrow(() -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization));
    }

    @Test
    void rejectsBasicPolicyOutsideAllowedStatuses() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("REWARDED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void givesFullScopePrecedenceAndPermitsFullPolicy() {
        enableEligibility();
        String authorization = authorization(
                "transaction:invoicelifecycle:basic transaction:invoicelifecycle:full");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("REWARDED", "NOT_REFUNDED", "REJECTED")));

        assertDoesNotThrow(() -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization));
    }

    @Test
    void rejectsMissingLifecycleScope() {
        enableEligibility();
        String authorization = authorization("other:scope");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsEligibilityForAnotherTransaction() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        Transaction transaction = transaction();
        RewardBatchEligibilityDTO eligibility = eligibility("INVOICED", "APPROVED", "CONSULTABLE");
        eligibility.setTransactionId("another-transaction");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsFullPolicyOutsideAllowedStatuses() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:full");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("CREATED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsEligibilityForAnotherInitiative() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        Transaction transaction = transaction();
        RewardBatchEligibilityDTO eligibility = eligibility("INVOICED", "APPROVED", "CONSULTABLE");
        eligibility.setInitiativeId("another-initiative");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsEligibilityForAnotherMerchant() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        Transaction transaction = transaction();
        RewardBatchEligibilityDTO eligibility = eligibility("INVOICED", "APPROVED", "CONSULTABLE");
        eligibility.setMerchantId("another-merchant");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsBasicPolicyWithAnInvalidBatchStatus() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:basic");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "REJECTED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsFullPolicyWithAnInvalidBatchStatus() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:full");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "REJECTED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsFullPolicyWithAnInvalidBatchTransactionStatus() {
        enableEligibility();
        String authorization = authorization("transaction:invoicelifecycle:full");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "UNKNOWN")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsMalformedBearerToken() {
        enableEligibility();
        String authorization = "Bearer malformed-token";
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsAuthorizationWithoutBearerToken() {
        enableEligibility();
        String authorization = "Basic token";
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsNullAuthorization() {
        enableEligibility();
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, null))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, null));
    }

    @Test
    void rejectsBearerTokenWithUnreadablePayload() {
        enableEligibility();
        String authorization = "Bearer header.%.signature";
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void appliesFullPolicyForArrayScopeClaim() {
        enableEligibility();
        String authorization = authorizationPayload("{\"scp\":[\"transaction:invoicelifecycle:full\"]}");
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("REWARDED", "NOT_REFUNDED", "REJECTED")));

        assertDoesNotThrow(() -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization));
    }

    @Test
    void rejectsBlankTextualScopeClaim() {
        enableEligibility();
        String authorization = authorization("\u2003");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsNonTextualAndNonArrayScopeClaim() {
        enableEligibility();
        String authorization = authorizationPayload("{\"scope\":1}");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    @Test
    void rejectsBlankAndNonTextualArrayScopeEntries() {
        enableEligibility();
        String authorization = authorizationPayload("{\"scp\":[\" \",1]}");
        Transaction transaction = transaction();
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, MERCHANT_ID, authorization));
    }

    private void enableEligibility() {
        rewardBatchImpact.getEligibility().setEnabled(true);
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setInitiativeId(INITIATIVE_ID);
        return transaction;
    }

    private RewardBatchEligibilityDTO eligibility(
            String transactionStatus, String batchStatus, String batchTransactionStatus) {
        RewardBatchEligibilityDTO eligibility = new RewardBatchEligibilityDTO();
        eligibility.setTransactionId(TRANSACTION_ID);
        eligibility.setInitiativeId(INITIATIVE_ID);
        eligibility.setMerchantId(MERCHANT_ID);
        eligibility.setTransactionStatus(transactionStatus);
        eligibility.setBatchStatus(batchStatus);
        eligibility.setBatchTransactionStatus(batchTransactionStatus);
        return eligibility;
    }

    private String authorization(String scope) {
        return authorizationPayload("{\"scope\":\"" + scope + "\"}");
    }

    private String authorizationPayload(String payload) {
        return "Bearer header."
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + ".signature";
    }
}
