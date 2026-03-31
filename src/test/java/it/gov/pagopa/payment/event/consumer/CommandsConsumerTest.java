package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.service.ProcessConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommandsConsumerTest {

    @Mock
    private ProcessConsumerService processConsumerService;

    @InjectMocks
    private CommandsConsumer commandsConsumer;

    private Consumer<QueueCommandOperationDTO> consumerCommands;

    private static final  String OPERATION_TYPE = "TESTOPERATIONTYPE";
    private static final   String ENTITY_ID = "ENTITYID";
    private static final  Instant OPERATION_TIME = Instant.now();

    @BeforeEach
     void setUp(){
        consumerCommands = commandsConsumer.consumerCommands(processConsumerService);
    }

    @Test
    void testConsumerCommands(){
        QueueCommandOperationDTO queueCommandOperationDTO = new QueueCommandOperationDTO(OPERATION_TYPE,ENTITY_ID,OPERATION_TIME);
        consumerCommands.accept(queueCommandOperationDTO);
        verify(processConsumerService).processCommand(queueCommandOperationDTO);

    }



}
