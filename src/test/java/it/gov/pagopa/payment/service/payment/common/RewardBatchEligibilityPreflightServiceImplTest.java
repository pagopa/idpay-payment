package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.rest.rewardbatch.RewardBatchConnector;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDecision;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.RewardBatchEligibilityNotAllowedException;
import it.gov.pagopa.payment.exception.custom.RewardBatchInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardBatchEligibilityPreflightServiceImplTest {

    private static final String TRANSACTION_ID = "transaction-id";
    private static final String AUTHORIZATION = "******";
    private static final RewardBatchEligibilityOperation OPERATION =
            RewardBatchEligibilityOperation.INVOICE_REPLACEMENT;

    @Mock
    private RewardBatchConnector rewardBatchConnector;

    private AppConfigurationProperties.RewardBatchImpact rewardBatchImpact;
    private RewardBatchEligibilityPreflightServiceImpl preflightService;

    @BeforeEach
    void setUp() {
        rewardBatchImpact = new AppConfigurationProperties.RewardBatchImpact();
        preflightService = new RewardBatchEligibilityPreflightServiceImpl(
                rewardBatchImpact, rewardBatchConnector);
    }

    @Test
    void doesNotCallEligibilityWhenDisabled() {
        preflightService.verifyEligibility(transaction(), OPERATION, AUTHORIZATION);

        verify(rewardBatchConnector, never())
                .getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION);
    }

    @Test
    void permitsAllowedDecisionWhenEnabled() {
        enableEligibility();
        when(rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION))
                .thenReturn(RewardBatchEligibilityDecision.ALLOWED);

        assertDoesNotThrow(
                () -> preflightService.verifyEligibility(transaction(), OPERATION, AUTHORIZATION));
    }

    @Test
    void rejectsDeniedDecisionWhenEnabled() {
        enableEligibility();
        Transaction transaction = transaction();
        when(rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION))
                .thenReturn(RewardBatchEligibilityDecision.DENIED);

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, OPERATION, AUTHORIZATION));
    }

    @Test
    void rejectsInvalidDecisionWhenEnabled() {
        enableEligibility();
        Transaction transaction = transaction();
        when(rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION))
                .thenReturn(null);

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> preflightService.verifyEligibility(transaction, OPERATION, AUTHORIZATION));
    }

    @Test
    void propagatesEndpointFailureWhenEnabled() {
        enableEligibility();
        Transaction transaction = transaction();
        RewardBatchInvocationException failure =
                new RewardBatchInvocationException("Eligibility unavailable", false, null);
        when(rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION))
                .thenThrow(failure);

        assertThrows(
                RewardBatchInvocationException.class,
                () -> preflightService.verifyEligibility(transaction, OPERATION, AUTHORIZATION));
    }

    private void enableEligibility() {
        rewardBatchImpact.getEligibility().setEnabled(true);
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        return transaction;
    }
}
