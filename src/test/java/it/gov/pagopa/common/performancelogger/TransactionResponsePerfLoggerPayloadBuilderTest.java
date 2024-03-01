package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionResponsePerfLoggerPayloadBuilderTest {

    @Test
    void testApply(){
        TransactionResponsePerfLoggerPayloadBuilder builder = new TransactionResponsePerfLoggerPayloadBuilder();
        TransactionResponse trx = TransactionResponseFaker.mockInstance(1);
        trx.setId("TRANSACTIONID1");
        String APPLY_STRING  = "TrxId " +trx.getId() +" status "+trx.getStatus();

        String result = builder.apply(trx);

        Assertions.assertEquals(APPLY_STRING, result);

    }
}
