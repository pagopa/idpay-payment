package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.rest.rewardbatch.RewardBatchConnector;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDecision;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.RewardBatchEligibilityNotAllowedException;
import org.springframework.stereotype.Service;

@Service
public class RewardBatchEligibilityPreflightServiceImpl implements RewardBatchEligibilityPreflightService {

    private final AppConfigurationProperties.RewardBatchImpact rewardBatchImpact;
    private final RewardBatchConnector rewardBatchConnector;

    public RewardBatchEligibilityPreflightServiceImpl(
            AppConfigurationProperties.RewardBatchImpact rewardBatchImpact,
            RewardBatchConnector rewardBatchConnector) {
        this.rewardBatchImpact = rewardBatchImpact;
        this.rewardBatchConnector = rewardBatchConnector;
    }

    @Override
    public void verifyEligibility(
            Transaction transaction,
            RewardBatchEligibilityOperation operation,
            String authorization) {
        if (!rewardBatchImpact.getEligibility().isEnabled()) {
            return;
        }

        RewardBatchEligibilityDecision decision =
                rewardBatchConnector.getEligibilityDecision(transaction.getId(), operation, authorization);
        if (RewardBatchEligibilityDecision.ALLOWED != decision) {
            throw new RewardBatchEligibilityNotAllowedException(
                    "The reward batch eligibility decision does not permit the invoice lifecycle operation");
        }
    }
}
