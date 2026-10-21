package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthPaymentDTOPerfLoggerPayloadBuilderTest {

    @Test
    void testApply(){
        AuthPaymentDTOPerfLoggerPayloadBuilder builder = new AuthPaymentDTOPerfLoggerPayloadBuilder();
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        String APPLY_STRING  = "TrxId " +trx.getId() +" status "+trx.getStatus();

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,trx);
        String result = builder.apply(authPaymentDTO);

        Assertions.assertEquals(APPLY_STRING, result);

    }
}
