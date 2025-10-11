package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.config.KafkaConfiguration;
import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentErrorNotifierServiceTest {

    public static final String ERROR_MESSAGE = "test";
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    @Mock
    private KafkaConfiguration kafkaConfigurationMock;

    private PaymentErrorNotifierService service;

    @BeforeEach
    void setUp() {
        service = new PaymentErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                kafkaConfigurationMock);
    }

    @Test
    void notifyAuthPayment() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID1");
        Message<TransactionInProgress> message = buildMessage(transaction, transaction.getUserId());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = mockKafkaConfig();

        Mockito.when(errorNotifierServiceMock.notify(eq(kafkaInfoDTO), eq(message), any(), eq(true), eq(false), any())).thenReturn(false);

        service.notifyAuthPayment(message,
                "[QR_CODE_AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void notifyCancelPayment() {
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        Message<TransactionInProgress> message = buildMessage(trx, trx.getUserId());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = mockKafkaConfig();

        Mockito.when(errorNotifierServiceMock.notify(eq(kafkaInfoDTO), eq(message), any(), eq(true), eq(false), any())).thenReturn(false);

        service.notifyCancelPayment(
                message,
                "[QR_CODE_CANCEL_PAYMENT] An error occurred while publishing the cancellation authorized result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any(), any(), any(), anyBoolean(), anyBoolean(), any());

    }

    @Test
    void notifyReversalPayment() {
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        Message<TransactionInProgress> message = buildMessage(trx, trx.getUserId());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = mockKafkaConfig();

        Mockito.when(errorNotifierServiceMock.notify(eq(kafkaInfoDTO), eq(message), any(), eq(true), eq(false), any())).thenReturn(false);

        service.notifyReversalPayment(
                message,
                "[QR_CODE_CANCEL_PAYMENT] An error occurred while publishing the reversal authorized result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any(), any(), any(), anyBoolean(), anyBoolean(), any());

    }

    @Test
    void notifyConfirmPayment(){
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        Message<TransactionInProgress> message = buildMessage(trx, trx.getUserId());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = mockKafkaConfig();

        Mockito.when(errorNotifierServiceMock.notify(eq(kafkaInfoDTO), eq(message), any(), eq(false), eq(false), any())).thenReturn(false);

        service.notifyConfirmPayment(
                message,
                "[QR_CODE_CONFIRM_PAYMENT] An error occurred while publishing the Confirm Payment result",
                false,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    private Message<TransactionInProgress> buildMessage(TransactionInProgress trx, String key) {
        return MessageBuilder.withPayload(trx)
            .setHeader(KafkaHeaders.KEY, key)
            .build();
    }

    private KafkaConfiguration.KafkaInfoDTO mockKafkaConfig() {
        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = new KafkaConfiguration.KafkaInfoDTO();
        kafkaInfoDTO.setDestination("destination");
        kafkaInfoDTO.setGroup("group");
        kafkaInfoDTO.setType("type");
        kafkaInfoDTO.setBrokers("brokers");

        KafkaConfiguration.Stream cloudStream = new KafkaConfiguration.Stream();
        cloudStream.setBindings(Map.of("transactionOutcome-out-0", kafkaInfoDTO));
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(cloudStream);
        return kafkaInfoDTO;
    }
}
