package it.gov.pagopa.payment.connector.rest.rewardbatch;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDecision;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityRequestDTO;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityResponseDTO;
import it.gov.pagopa.payment.exception.custom.RewardBatchInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardBatchConnectorImplTest {

    private static final String TRANSACTION_ID = "transaction-id";
    private static final String AUTHORIZATION = "******";
    private static final RewardBatchEligibilityOperation OPERATION =
            RewardBatchEligibilityOperation.INVOICE_REPLACEMENT;

    @Mock
    private RewardBatchRestClient restClient;
    @InjectMocks
    private RewardBatchConnectorImpl rewardBatchConnector;

    @Test
    void getEligibilityDecisionReturnsResponseAndForwardsRequest() {
        RewardBatchEligibilityResponseDTO response = response(RewardBatchEligibilityDecision.ALLOWED);
        RewardBatchEligibilityRequestDTO request = new RewardBatchEligibilityRequestDTO(OPERATION);
        when(restClient.getEligibilityDecision(TRANSACTION_ID, request, AUTHORIZATION))
                .thenReturn(ResponseEntity.ok(response));

        RewardBatchEligibilityDecision result =
                rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION);

        assertEquals(RewardBatchEligibilityDecision.ALLOWED, result);
        verify(restClient).getEligibilityDecision(TRANSACTION_ID, request, AUTHORIZATION);
    }

    @Test
    void getEligibilityDecisionReturnsDeniedDecision() {
        RewardBatchEligibilityRequestDTO request = new RewardBatchEligibilityRequestDTO(OPERATION);
        when(restClient.getEligibilityDecision(TRANSACTION_ID, request, AUTHORIZATION))
                .thenReturn(ResponseEntity.ok(response(RewardBatchEligibilityDecision.DENIED)));

        assertEquals(
                RewardBatchEligibilityDecision.DENIED,
                rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION));
    }

    @Test
    void getEligibilityDecisionRejectsMissingBody() {
        RewardBatchEligibilityRequestDTO request = new RewardBatchEligibilityRequestDTO(OPERATION);
        when(restClient.getEligibilityDecision(TRANSACTION_ID, request, AUTHORIZATION))
                .thenReturn(ResponseEntity.noContent().build());

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION));
    }

    @Test
    void getEligibilityDecisionRejectsMissingDecision() {
        RewardBatchEligibilityRequestDTO request = new RewardBatchEligibilityRequestDTO(OPERATION);
        when(restClient.getEligibilityDecision(TRANSACTION_ID, request, AUTHORIZATION))
                .thenReturn(ResponseEntity.ok(new RewardBatchEligibilityResponseDTO()));

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION));
    }

    @Test
    void getEligibilityDecisionRejectsClientAndServerFailures() {
        Request request = Request.create(
                Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        when(restClient.getEligibilityDecision(
                        TRANSACTION_ID,
                        new RewardBatchEligibilityRequestDTO(OPERATION),
                        AUTHORIZATION))
                .thenThrow(new FeignException.InternalServerError("", request, null, null));

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION));
    }

    @Test
    void getEligibilityDecisionRejectsTimeouts() {
        Request request = Request.create(
                Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        when(restClient.getEligibilityDecision(
                        TRANSACTION_ID,
                        new RewardBatchEligibilityRequestDTO(OPERATION),
                        AUTHORIZATION))
                .thenThrow(new RetryableException(
                        -1, "timeout", Request.HttpMethod.POST, new RuntimeException("timeout"), (Long) null, request));

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.getEligibilityDecision(TRANSACTION_ID, OPERATION, AUTHORIZATION));
    }

    private RewardBatchEligibilityResponseDTO response(RewardBatchEligibilityDecision decision) {
        RewardBatchEligibilityResponseDTO response = new RewardBatchEligibilityResponseDTO();
        response.setDecision(decision);
        return response;
    }
}
