package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
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

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonCancelServiceTest {

    private final long cancelExpirationMinutes = 5;

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;

    private CommonCancelServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new CommonCancelServiceImpl (
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
            Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", e.getCode());
            Assertions.assertEquals("Cannot find transaction with transactionId [TRXID]", e.getMessage());
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
            Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("The merchant with id [MERCHANTID0] associated to the transaction is not equal to the merchant with id [MERCHID]", e.getMessage());
        }
    }

    @Test
    void testAcquirerIdNotValid() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.cancelTransaction("TRXID", "MERCHID_1", "ACQID");
            Assertions.fail("Expected exception");
        } catch (MerchantOrAcquirerNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("The merchant with id [MERCHID] associated to the transaction is not equal to the merchant with id [MERCHID_1]", e.getMessage());
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
            Assertions.assertEquals(ExceptionCode.TRX_DELETE_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("Cannot cancel transaction with transactionId [TRXID]", e.getMessage());
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
            Assertions.assertEquals("Cannot cancel expired transaction with transactionId [TRXID]", e.getMessage());
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
