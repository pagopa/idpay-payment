package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonConfirmServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSynchronizer transactionSynchronizer;


    private final TransactionInProgress2TransactionResponseMapper mapper = new TransactionInProgress2TransactionResponseMapper(5, "qrcodeImgBaseUrl", "qrcodeImgBaseUrl");

    CommonConfirmServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new CommonConfirmServiceImpl(
                        transactionRepository,
                        repositoryMock,
                        mapper,
                        notifierServiceMock,
                        paymentErrorNotifierServiceMock,
                        auditUtilitiesMock,
                        transactionSynchronizer);
    }

    @Test
    void testTrxNotFound() {
        TransactionNotFoundOrExpiredException exception = Assertions.assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> service.confirmPayment("TRXID", "MERCHID", "ACQID")
        );

        Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", exception.getCode());
        Assertions.assertEquals("Cannot find transaction with transactionId [TRXID]", exception.getMessage());
    }

    @Test
    void testMerchantIdNotValid() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repositoryMock.findById("TRXID"))
                .thenReturn(Optional.ofNullable(TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED)));

        MerchantOrAcquirerNotAllowedException exception = Assertions.assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> service.confirmPayment("TRXID", "MERCHID", "ACQID")
        );

        Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, exception.getCode());
        Assertions.assertEquals(
                "The merchant with id [MERCHANTID0] associated to the transaction is not equal to the merchant with id [MERCHID]",
                exception.getMessage()
        );
    }

    @Test
    void testAcquirerIdNotValid() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);

        trx.setMerchantId("MERCHID");
        when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

        MerchantOrAcquirerNotAllowedException exception = Assertions.assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> service.confirmPayment("TRXID", "MERCHID_2", "ACQID")
        );

        Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, exception.getCode());
        Assertions.assertEquals(
                "The merchant with id [MERCHID] associated to the transaction is not equal to the merchant with id [MERCHID_2]",
                exception.getMessage()
        );
    }

    @Test
    void testStatusNotValid() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);

        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

        OperationNotAllowedException exception = Assertions.assertThrows(
                OperationNotAllowedException.class,
                () -> service.confirmPayment("TRXID", "MERCHID", "ACQID")
        );

        Assertions.assertEquals(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, exception.getCode());
        Assertions.assertEquals("Cannot operate on transaction with transactionId [TRXID] in status CREATED", exception.getMessage());
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
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setRewardCents(1000L);

        when(repositoryMock.findById("TRXID")).thenReturn(Optional.of(trx));

        when(notifierServiceMock.notify(trx, trx.getMerchantId())).thenReturn(transactionOutcome);

        TransactionResponse result = service.confirmPayment("TRXID", "MERCHID", "ACQID");

        Assertions.assertEquals(result, mapper.apply(trx));
        Assertions.assertEquals(SyncTrxStatus.REWARDED, result.getStatus());
    }
}
