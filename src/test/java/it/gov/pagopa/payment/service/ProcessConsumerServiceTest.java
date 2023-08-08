package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ProcessConsumerServiceTest {
    private ProcessConsumerService processConsumerService;
    @Mock private TransactionInProgressRepository transactionInProgressRepository;
    @Mock private AuditUtilities auditUtilities;

    @BeforeEach
    void setUp() {
        processConsumerService = new ProcessConsumerServiceImpl(transactionInProgressRepository, auditUtilities);
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processConsumer_deleteTransactions(String operationType, int times) {
        QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder()
                .entityId("INITIATIVE_ID")
                .operationType(operationType)
                .build();

        TransactionInProgress transaction = new TransactionInProgress();
        transaction.setInitiativeId("INITIATIVE_ID");

        if(operationType.equals("DELETE_INITIATIVE")){
            Mockito.when(transactionInProgressRepository.deleteByInitiativeId(queueCommandOperationDTO.getEntityId()))
                    .thenReturn(List.of(transaction));
        }

        processConsumerService.processCommand(queueCommandOperationDTO);

        Mockito.verify(transactionInProgressRepository, Mockito.times(times)).deleteByInitiativeId(queueCommandOperationDTO.getEntityId());
    }

    private static Stream<Arguments> operationTypeAndInvocationTimes() {
        return Stream.of(
                Arguments.of("DELETE_INITIATIVE", 1),
                Arguments.of("OPERATION_TYPE_TEST", 0)
        );
    }
}
