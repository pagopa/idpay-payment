package it.gov.pagopa.payment.service.payment.common;

import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
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

@ExtendWith(MockitoExtension.class)
class CommonConfirmServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;


    private final TransactionInProgress2TransactionResponseMapper mapper = new TransactionInProgress2TransactionResponseMapper(5, "qrcodeImgBaseUrl", "qrcodeImgBaseUrl");

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
            service.confirmPayment("TRXID", "MERCHID", "ACQID");
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
            service.confirmPayment("TRXID", "MERCHID_2", "ACQID");
            Assertions.fail("Expected exception");
        } catch (MerchantOrAcquirerNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("The merchant with id [MERCHID] associated to the transaction is not equal to the merchant with id [MERCHID_2]", e.getMessage());
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
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("Cannot operate on transaction with transactionId [TRXID] in status CREATED", e.getMessage());
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

        Assertions.assertEquals(result, mapper.apply(trx));
        Assertions.assertEquals(SyncTrxStatus.REWARDED, result.getStatus());
    }
}
