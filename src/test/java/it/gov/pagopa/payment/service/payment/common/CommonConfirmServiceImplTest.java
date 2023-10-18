package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonConfirmServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock
    private TransactionInProgress2TransactionResponseMapper mapper;

    CommonConfirmServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new CommonConfirmServiceImpl(
                        repositoryMock,
                        mapper,
                        notifierServiceMock,
                        paymentErrorNotifierServiceMock,
                        auditUtilitiesMock);
    }

    @Test
    void testTrxNotFound() {
        try {
            service.confirmPayment("TRXID", "MERCHID", "ACQID");
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
            service.confirmPayment("TRXID", "MERCHID", "ACQID");
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
            service.confirmPayment("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        }
    }

    @Test
    void testStatusNotValid() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try {
            service.confirmPayment("TRXID", "MERCHID", "ACQID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        }
    }

    @Test
    void testSuccess() {
        testSuccessful(true);
    }

    @Test
    void testSuccessNotNotified() {
        testSuccessful(false);
    }

    private void testSuccessful(boolean transactionOutcome) {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setReward(1000L);
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        when(notifierServiceMock.notify(trx, trx.getMerchantId())).thenReturn(transactionOutcome);

        TransactionResponse result = service.confirmPayment("TRXID", "MERCHID", "ACQID");
        System.out.println(result);
        Assertions.assertEquals(result, mapper.apply(trx));
        Assertions.assertEquals(SyncTrxStatus.REWARDED, result.getStatus());
    }
}
