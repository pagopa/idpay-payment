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
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("REWARDED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization));
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
        when(rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, authorization))
                .thenReturn(Optional.of(eligibility("INVOICED", "APPROVED", "CONSULTABLE")));

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction(), MERCHANT_ID, authorization));
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
        String payload = "{\"scope\":\"" + scope + "\"}";
        return "Bearer header."
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + ".signature";
    }
}
