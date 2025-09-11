package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.PreviewPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewPaymentDTOPerfLoggerPayloadBuilderTest {

    @Test
    void testApply() {
        PreviewPaymentDTOPerfLoggerPayloadBuilder builder = new PreviewPaymentDTOPerfLoggerPayloadBuilder();
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        String APPLY_STRING = "TrxId " + trx.getId() + " status " + trx.getStatus();

        PreviewPaymentDTO previewPaymentDTO = PreviewPaymentDTOFaker.mockInstance();
        String result = builder.apply(previewPaymentDTO);

        Assertions.assertEquals(APPLY_STRING, result);

    }
}
