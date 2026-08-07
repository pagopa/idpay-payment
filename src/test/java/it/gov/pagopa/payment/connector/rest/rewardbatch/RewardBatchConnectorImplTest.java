package it.gov.pagopa.payment.connector.rest.rewardbatch;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;
import it.gov.pagopa.payment.exception.custom.RewardBatchInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardBatchConnectorImplTest {

    private static final String MERCHANT_ID = "merchant-id";
    private static final String TRANSACTION_ID = "transaction-id";
    private static final String AUTHORIZATION = "Bearer token";

    @Mock
    private RewardBatchRestClient restClient;
    @InjectMocks
    private RewardBatchConnectorImpl rewardBatchConnector;

    @Test
    void findEligibilityReturnsResponseAndForwardsAuthorization() {
        RewardBatchEligibilityDTO eligibility = new RewardBatchEligibilityDTO();
        eligibility.setTransactionId(TRANSACTION_ID);
        when(restClient.findEligibility(TRANSACTION_ID, MERCHANT_ID, AUTHORIZATION))
                .thenReturn(ResponseEntity.ok(eligibility));

        Optional<RewardBatchEligibilityDTO> result =
                rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, AUTHORIZATION);

        assertEquals(Optional.of(eligibility), result);
        verify(restClient).findEligibility(TRANSACTION_ID, MERCHANT_ID, AUTHORIZATION);
    }

    @Test
    void findEligibilityMapsNoContentToEmptyResult() {
        when(restClient.findEligibility(TRANSACTION_ID, MERCHANT_ID, AUTHORIZATION))
                .thenReturn(ResponseEntity.noContent().build());

        Optional<RewardBatchEligibilityDTO> result =
                rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, AUTHORIZATION);

        assertEquals(Optional.empty(), result);
    }

    @Test
    void findEligibilityRejectsClientAndServerFailures() {
        Request request = Request.create(
                Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        when(restClient.findEligibility(TRANSACTION_ID, MERCHANT_ID, AUTHORIZATION))
                .thenThrow(new FeignException.InternalServerError("", request, null, null));

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, AUTHORIZATION));
    }

    @Test
    void findEligibilityRejectsTimeouts() {
        Request request = Request.create(
                Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        when(restClient.findEligibility(TRANSACTION_ID, MERCHANT_ID, AUTHORIZATION))
                .thenThrow(new RetryableException(
                        -1, "timeout", Request.HttpMethod.GET, new RuntimeException("timeout"), (Long) null, request));

        assertThrows(
                RewardBatchInvocationException.class,
                () -> rewardBatchConnector.findEligibility(MERCHANT_ID, TRANSACTION_ID, AUTHORIZATION));
    }
}
