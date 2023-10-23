package it.gov.pagopa.payment.service.payment.qrcode;

import static org.mockito.Mockito.when;

import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
        } catch (TransactionNotFoundOrExpiredException e) {
            Assertions.assertEquals("PAYMENT_NOT_FOUND_EXPIRED", e.getCode());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Cannot found transaction having id: TRXID", e.getMessage());
        }
    }

    @Test
    void testMerchantIdNotValid() {
        when(repositoryMock.findByIdThrottled("TRXID"))
                .thenReturn(TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED));

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (MerchantOrAcquirerNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_OR_ACQUIRER_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Requesting merchantId (MERCHID through acquirer ACQID) not allowed to operate on transaction having id TRXID", e.getMessage());
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
        } catch (MerchantOrAcquirerNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_OR_ACQUIRER_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Requesting merchantId (MERCHID through acquirer ACQID) not allowed to operate on transaction having id TRXID", e.getMessage());
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
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_STATUS_NOT_VALID, e.getCode());
            Assertions.assertEquals("[CANCEL_TRANSACTION] Cannot cancel confirmed transaction: id TRXID", e.getMessage());
        }
    }

    @Test
    void testTrxExpired() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setTrxDate(OffsetDateTime.now().minusMinutes(cancelExpirationMinutes+1));
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.cancelTransaction("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.PAYMENT_TRANSACTION_EXPIRED, e.getCode());
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
