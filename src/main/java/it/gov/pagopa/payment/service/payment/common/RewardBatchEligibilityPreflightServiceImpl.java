package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.rest.rewardbatch.RewardBatchConnector;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.RewardBatchEligibilityNotAllowedException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class RewardBatchEligibilityPreflightServiceImpl implements RewardBatchEligibilityPreflightService {

    private static final String BASIC_SCOPE = "transaction:invoicelifecycle:basic";
    private static final String FULL_SCOPE = "transaction:invoicelifecycle:full";
    private static final Set<String> BASIC_BATCH_STATUSES = Set.of("CREATED", "EVALUATING", "APPROVED");
    private static final Set<String> FULL_TRANSACTION_STATUSES = Set.of(SyncTrxStatus.INVOICED.name(), SyncTrxStatus.REWARDED.name());
    private static final Set<String> FULL_BATCH_STATUSES =
            Set.of("CREATED", "EVALUATING", "APPROVED", "PENDING_REFUND", "REFUNDED", "NOT_REFUNDED");
    private static final Set<String> FULL_BATCH_TRANSACTION_STATUSES =
            Set.of("CONSULTABLE", "TO_CHECK", "SUSPENDED", "REJECTED");

    private final AppConfigurationProperties.RewardBatchImpact rewardBatchImpact;
    private final RewardBatchConnector rewardBatchConnector;
    private final ObjectMapper objectMapper;

    public RewardBatchEligibilityPreflightServiceImpl(
            AppConfigurationProperties.RewardBatchImpact rewardBatchImpact,
            RewardBatchConnector rewardBatchConnector,
            ObjectMapper objectMapper) {
        this.rewardBatchImpact = rewardBatchImpact;
        this.rewardBatchConnector = rewardBatchConnector;
        this.objectMapper = objectMapper;
    }

    @Override
    public void verifyEligibility(Transaction transaction, String merchantId, String authorization) {
        if (!rewardBatchImpact.getEligibility().isEnabled()) {
            return;
        }

        rewardBatchConnector.findEligibility(merchantId, transaction.getId(), authorization)
                .ifPresent(eligibility -> validateEligibility(transaction, merchantId, authorization, eligibility));
    }

    private void validateEligibility(
            Transaction transaction,
            String merchantId,
            String authorization,
            RewardBatchEligibilityDTO eligibility) {
        if (!Objects.equals(transaction.getId(), eligibility.getTransactionId())
                || !Objects.equals(transaction.getInitiativeId(), eligibility.getInitiativeId())
                || !Objects.equals(merchantId, eligibility.getMerchantId())) {
            throw new RewardBatchEligibilityNotAllowedException(
                    "The reward batch eligibility response does not match the requested transaction");
        }

        Set<String> scopes = extractScopes(authorization);
        if (scopes.contains(FULL_SCOPE)) {
            validateFullPolicy(eligibility);
            return;
        }
        if (scopes.contains(BASIC_SCOPE)) {
            validateBasicPolicy(eligibility);
            return;
        }
        throw new RewardBatchEligibilityNotAllowedException(
                "The caller does not have a supported invoice lifecycle scope");
    }

    private void validateBasicPolicy(RewardBatchEligibilityDTO eligibility) {
        if (!"INVOICED".equals(eligibility.getTransactionStatus())
                || !BASIC_BATCH_STATUSES.contains(eligibility.getBatchStatus())) {
            throw new RewardBatchEligibilityNotAllowedException(
                    "The reward batch eligibility does not permit the basic invoice lifecycle policy");
        }
    }

    private void validateFullPolicy(RewardBatchEligibilityDTO eligibility) {
        if (!FULL_TRANSACTION_STATUSES.contains(eligibility.getTransactionStatus())
                || !FULL_BATCH_STATUSES.contains(eligibility.getBatchStatus())
                || !FULL_BATCH_TRANSACTION_STATUSES.contains(eligibility.getBatchTransactionStatus())) {
            throw new RewardBatchEligibilityNotAllowedException(
                    "The reward batch eligibility does not permit the full invoice lifecycle policy");
        }
    }

    private Set<String> extractScopes(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RewardBatchEligibilityNotAllowedException("The Authorization header does not contain a bearer token");
        }

        String[] tokenParts = authorization.substring("Bearer ".length()).split("\\.");
        if (tokenParts.length != 3) {
            throw new RewardBatchEligibilityNotAllowedException("The bearer token is malformed");
        }

        try {
            JsonNode payload = objectMapper.readTree(
                    new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8));
            Set<String> scopes = new HashSet<>();
            addScopes(scopes, payload.get("scope"));
            addScopes(scopes, payload.get("scp"));
            return scopes;
        } catch (IllegalArgumentException | JacksonException e) {
            throw new RewardBatchEligibilityNotAllowedException("The bearer token scopes cannot be read");
        }
    }

    private void addScopes(Set<String> scopes, JsonNode scopeClaim) {
        if (scopeClaim == null) {
            return;
        }
        if (scopeClaim.isTextual()) {
            for (String scope : scopeClaim.asText().split("\\s+")) {
                if (!scope.isBlank()) {
                    scopes.add(scope);
                }
            }
            return;
        }
        if (scopeClaim.isArray()) {
            for (JsonNode scope : scopeClaim) {
                if (scope.isTextual() && !scope.asText().isBlank()) {
                    scopes.add(scope.asText());
                }
            }
        }
    }
}
