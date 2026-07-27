package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.service.ProcessConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommandsConsumerTest {

    private static final String OPERATION_TYPE = "TESTOPERATIONTYPE";
    private static final String ENTITY_ID = "ENTITYID";

    @Mock
    private ProcessConsumerService processConsumerService;

    @InjectMocks
    private CommandsConsumer commandsConsumer;

    private Consumer<QueueCommandOperationDTO> consumerCommands;

    @BeforeEach
    void setUp() {
        consumerCommands = commandsConsumer.consumerCommands(processConsumerService);
    }

    @Test
    void testConsumerCommands() {
        LocalDateTime operationTime = LocalDateTime.now(ZoneId.of("Europe/Rome"));
        QueueCommandOperationDTO queueCommandOperationDTO = new QueueCommandOperationDTO(OPERATION_TYPE, ENTITY_ID, operationTime);

        consumerCommands.accept(queueCommandOperationDTO);

        verify(processConsumerService).processCommand(queueCommandOperationDTO);
    }
}