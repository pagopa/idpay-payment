package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentErrorNotifierServiceTest {

    public static final String BUILDER_MESSAGING_SERVICE = "builderMessagingService";
    public static final String NOTIFICATIONBUILDER = "notificationbuilder";
    public static final String TOPIC = "topic";
    public static final String ERROR_MESSAGE = "test";
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private PaymentErrorNotifierService service;

    @BeforeEach
    void setUp() {
        service = new PaymentErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                BUILDER_MESSAGING_SERVICE,
                NOTIFICATIONBUILDER,
                TOPIC
        );
    }

    @Test
    void notifyAuthPayment() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID1");

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

        Mockito.when(errorNotifierServiceMock.notify(any())).thenReturn(false);

        service.notifyAuthPayment(buildMessage(transaction, transaction.getUserId()),
                "[QR_CODE_AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any());
    }

    @Test
    void notifyCancelPayment() {
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        Mockito.when(errorNotifierServiceMock.notify(any())).thenReturn(false);

        service.notifyCancelPayment(
                buildMessage(trx, trx.getMerchantId()),
                "[QR_CODE_CANCEL_PAYMENT] An error occurred while publishing the cancellation authorized result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any());

    }

    @Test
    void notifyConfirmPayment(){
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        Mockito.when(errorNotifierServiceMock.notify(any())).thenReturn(false);

        service.notifyConfirmPayment(
                buildMessage(trx, trx.getMerchantId()),
                "[QR_CODE_CONFIRM_PAYMENT] An error occurred while publishing the Confirm Payment result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(errorNotifierServiceMock).notify(any());
    }

    private Message<TransactionInProgress> buildMessage(TransactionInProgress trx, String key) {
        return MessageBuilder.withPayload(trx)
            .setHeader(KafkaHeaders.KEY, key)
            .build();
    }
}
