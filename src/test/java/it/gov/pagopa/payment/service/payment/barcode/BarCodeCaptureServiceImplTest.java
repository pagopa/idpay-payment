package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodeCaptureServiceImplTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionBarCodeInProgress2TransactionResponseMapper mapper;

    BarCodeCaptureServiceImpl service;

    @BeforeEach
    void init() {
        service =
                new BarCodeCaptureServiceImpl(
                        repositoryMock,
                        mapper,
                        auditUtilitiesMock);
    }

    @Test
    void testCapturePaymentTrxNotFound() {
        try {
            service.capturePayment("trxCode");
            Assertions.fail("Expected exception");
        } catch (TransactionNotFoundOrExpiredException e) {
            Assertions.assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", e.getCode());
            Assertions.assertEquals("Cannot find transaction with transactionCode [trxCode]", e.getMessage());
        }
    }

    @Test
    void testCapturePaymentStatusNotValid() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.CREATED);
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));
        try {
            service.capturePayment("trxCode");
            Assertions.fail("Expected exception");
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_OPERATION_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("Cannot operate on transaction with transactionCode [trxCode] in status CREATED", e.getMessage());
        }
    }

    @Test
    void testCapturePayment() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        trx.setAcquirerId("ACQID");
        trx.setStatus(SyncTrxStatus.AUTHORIZED);
        when(repositoryMock.findByTrxCode(any())).thenReturn(Optional.of(trx));

        TransactionBarCodeResponse result = service.capturePayment("trxCode");

        Assertions.assertEquals(result, mapper.apply(trx));
    }

}
