package it.gov.pagopa.payment.service;

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
import org.springframework.cloud.stream.function.StreamBridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErrorNotifierServiceTest {

    public static final String BUILDER_MESSAGING_SERVICE = "builderMessagingService";
    public static final String APP_NAME = "appName";
    public static final String NOTIFICATIONBUILDER = "notificationbuilder";
    public static final String TOPIC = "topic";
    public static final String ERROR_MESSAGE = "test";
    @Mock
    private StreamBridge streamBridgeMock;
    private ErrorNotifierService errorNotifierService;

    @BeforeEach
    void setUp() {
        errorNotifierService = new ErrorNotifierServiceImpl(
                streamBridgeMock,
                APP_NAME,
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

        Mockito.when(streamBridgeMock.send(anyString(), any())).thenReturn(false);

        errorNotifierService.notifyAuthPayment(
                TransactionNotifierServiceImpl.buildMessage(transaction, transaction.getUserId()),
                "[QR_CODE_AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(streamBridgeMock).send(anyString(), any());
    }

    @Test
    void notifyConfirmPayment(){
        TransactionInProgress trx= TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        Mockito.when(streamBridgeMock.send(anyString(),any())).thenReturn(false);

        errorNotifierService.notifyConfirmPayment(
                TransactionNotifierServiceImpl.buildMessage(trx, trx.getMerchantId()),
                "[QR_CODE_CONFIRM_PAYMENT] An error occurred while publishing the Confirm Payment result",
                true,
                new Throwable(ERROR_MESSAGE)
        );

        verify(streamBridgeMock).send(anyString(),any());
    }
}
