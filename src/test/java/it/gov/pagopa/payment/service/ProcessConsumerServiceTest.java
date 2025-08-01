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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ProcessConsumerServiceTest {
    private ProcessConsumerService processConsumerService;
    @Mock
    private TransactionInProgressRepository transactionInProgressRepository;
    @Mock
    private AuditUtilities auditUtilities;
    public static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";
    private static final String TRX_ID = "TRX_ID";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final int PAGE_SIZE = 100;

    @BeforeEach
    void setUp() {
        processConsumerService = new ProcessConsumerServiceImpl(transactionInProgressRepository,auditUtilities,PAGE_SIZE, 1000);
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processCommand_deleteTransactions(String operationType, int times) {
        // Given
        final QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(operationType)
                .operationTime(LocalDateTime.now().minusMinutes(5))
                .build();
        TransactionInProgress trxInProgress = TransactionInProgress.builder()
                .id(TRX_ID)
                .initiativeId(INITIATIVE_ID)
                .initiatives(List.of(INITIATIVE_ID))
                .build();
        final List<TransactionInProgress> deletedPage = List.of(trxInProgress);

        if(times == 2){
            final List<TransactionInProgress> trxPage = createTransactionInProgressPage(PAGE_SIZE);
            when(transactionInProgressRepository.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(trxPage)
                    .thenReturn(deletedPage);
        } else if (times == 1){
            when(transactionInProgressRepository.deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE))
                    .thenReturn(deletedPage);
        }

        // When
        if (times == 2){
            Thread.currentThread().interrupt();
        }
        processConsumerService.processCommand(queueCommandOperationDTO);

        // Then
        Mockito.verify(transactionInProgressRepository, Mockito.times(times)).deletePaged(queueCommandOperationDTO.getEntityId(), PAGE_SIZE);
    }

    private static Stream<Arguments> operationTypeAndInvocationTimes() {
        return Stream.of(
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 1),
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 2),
                Arguments.of("OPERATION_TYPE_TEST", 0)
        );
    }

    private List<TransactionInProgress> createTransactionInProgressPage(int PAGE_SIZE){
        List<TransactionInProgress> trxPage = new ArrayList<>();

        for(int i=0;i<PAGE_SIZE; i++){
            trxPage.add(TransactionInProgress.builder()
                    .id(TRX_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiatives(List.of(INITIATIVE_ID))
                    .build());
        }

        return trxPage;
    }
}
