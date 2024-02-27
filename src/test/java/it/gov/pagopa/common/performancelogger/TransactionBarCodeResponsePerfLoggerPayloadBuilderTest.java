package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.performancelogger.TransactionBarCodeResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeResponseFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionBarCodeResponsePerfLoggerPayloadBuilderTest {

    @Test
    void testApply(){
        TransactionBarCodeResponsePerfLoggerPayloadBuilder builder = new TransactionBarCodeResponsePerfLoggerPayloadBuilder();
        TransactionBarCodeResponse trx = TransactionBarCodeResponseFaker.mockInstance(1);
        trx.setId("TRANSACTIONID1");
        String APPLY_STRING  = "TrxId " +trx.getId() +" status "+trx.getStatus();

        String result = builder.apply(trx);

        Assertions.assertEquals(APPLY_STRING, result);

    }
}
