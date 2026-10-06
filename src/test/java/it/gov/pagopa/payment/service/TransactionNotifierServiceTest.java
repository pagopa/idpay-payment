package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierServiceImpl;
import it.gov.pagopa.payment.connector.event.trx.RewardTransactionDTO;
import it.gov.pagopa.payment.connector.event.trx.RewardTransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionNotifierServiceTest {

    private final String BINDER ="transaction-outcome";
    @Mock
    private StreamBridge streamBridgeMock;

    private TransactionNotifierService service;

    @BeforeEach
    void init() {

        service = new TransactionNotifierServiceImpl(streamBridgeMock, new RewardTransactionMapper(), BINDER);
    }

    @Test
    void notifiesWithTheContractDtoAndExistingBindingAndKey(){
        // Given
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setTransactionRevision(7L);
        trx.setCounterVersion(99L);
        // When
        boolean result = service.notify(trx,"TEST");

        verify(streamBridgeMock).send(eq("transactionOutcome-out-0"), eq(BINDER),
                argThat((Message<RewardTransactionDTO> message) ->
                        "TEST".equals(message.getHeaders().get("kafka_messageKey"))
                                && message.getPayload().getTransactionRevision() == 7L
                                && message.getPayload().getCounterVersion() == 99L
                                && message.getPayload().getMerchantId().equals(trx.getMerchantId())));

        // Then
        Assertions.assertFalse(result);
    }

    @Test
    void mapsTheExactGenericSnapshotShapeWithoutRewardBatchFields() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId("transaction-id");
        transaction.setMerchantId("merchant-id");
        transaction.setInitiativeId("initiative-id");
        transaction.setInitiatives(List.of("initiative-id"));
        transaction.setCounterVersion(99L);
        transaction.setTransactionRevision(7L);
        transaction.setInitiativeRejectionReasons(null);
        transaction.setAdditionalProperties(null);

        RewardTransactionDTO dto = new RewardTransactionMapper().transactionToRewardTransaction(transaction);

        String json = new ObjectMapper().writeValueAsString(dto);

        Assertions.assertEquals(
                "{\"id\":\"transaction-id\",\"merchantId\":\"merchant-id\",\"initiativeId\":\"initiative-id\","
                        + "\"initiatives\":[\"initiative-id\"],\"counterVersion\":99,\"transactionRevision\":7}",
                json);
        Assertions.assertFalse(json.contains("rewardBatchId"));
        Assertions.assertFalse(json.contains("rewardBatchStatusTrx"));
    }

    @Test
    void mapsNullLegacyRevisionToZeroWithoutUsingCounterVersion() {
        Transaction transaction = new Transaction();
        transaction.setInitiativeId("initiative-id");
        transaction.setInitiatives(List.of("initiative-id"));
        transaction.setCounterVersion(42L);

        RewardTransactionDTO dto = new RewardTransactionMapper().transactionToRewardTransaction(transaction);

        Assertions.assertEquals(0L, dto.getTransactionRevision());
        Assertions.assertEquals(42L, dto.getCounterVersion());
    }

    @Test
    void rejectsSnapshotsWithMultipleInitiatives() {
        Transaction transaction = new Transaction();
        transaction.setInitiativeId("initiative-id");
        transaction.setInitiatives(List.of("initiative-id", "another-initiative-id"));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RewardTransactionMapper().transactionToRewardTransaction(transaction));

    }


}
