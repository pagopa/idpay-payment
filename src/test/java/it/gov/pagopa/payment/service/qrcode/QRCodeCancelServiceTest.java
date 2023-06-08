package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeCancelServiceTest {

    private final long cancelExpirationMinutes = 5;

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;

    private QRCodeCancelService service;

    @BeforeEach
    void init() {
        service =
                new QRCodeCancelServiceImpl(
                        cancelExpirationMinutes,
                        repositoryMock,
                        rewardCalculatorConnectorMock,
                        notifierServiceMock,
                        paymentErrorNotifierServiceMock,
                        auditUtilitiesMock);
    }

    @Test
    void testTrxNotFound() {
        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
        }
    }

    @Test
    void testMerchantIdNotValid() {
        when(repositoryMock.findByIdThrottled("TRXID"))
                .thenReturn(TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED));

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        }
    }

    @Test
    void testAcquirerIdNotValid() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        }
    }

    @Test
    void testStatusNotValid() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.REWARDED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Cannot cancel confirmed transaction: id TRXID", e.getMessage());
        }
    }

    @Test
    void testTrxExpired() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setTrxChargeDate(OffsetDateTime.now().minusMinutes(cancelExpirationMinutes+1));
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Cannot cancel expired transaction: id TRXID", e.getMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED,false",
            "IDENTIFIED,false",
            "AUTHORIZED,false",
            "AUTHORIZED,true",
    })
    void testSuccessful(SyncTrxStatus status, boolean notifyOutcome) {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, status);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setReward(1000L);
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        boolean expectedNotify = SyncTrxStatus.AUTHORIZED.equals(status);

        if(expectedNotify){
            when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenReturn(new AuthPaymentDTO());
            when(notifierServiceMock.notify(trx, trx.getMerchantId())).thenReturn(notifyOutcome);
        }

        service.cancelTransaction("TRXID", "MERCHID", "ACQID");

        Mockito.verify(repositoryMock).deleteById("TRXID");
        if(expectedNotify && !notifyOutcome){
            Mockito.verify(paymentErrorNotifierServiceMock).notifyCancelPayment(Mockito.any(), Mockito.any(), Mockito.eq(true), Mockito.any());
        }
    }
}
