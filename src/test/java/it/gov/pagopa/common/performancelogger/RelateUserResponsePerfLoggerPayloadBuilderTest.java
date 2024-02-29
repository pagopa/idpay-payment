package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.performancelogger.RelateUserResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelateUserResponsePerfLoggerPayloadBuilderTest {

    @Test
    void testApply(){
        RelateUserResponseMapper mapper = new RelateUserResponseMapper();

        RelateUserResponsePerfLoggerPayloadBuilder builder = new RelateUserResponsePerfLoggerPayloadBuilder();
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        RelateUserResponse resultMapper = mapper.transactionMapper(trx);
        String EXPECTED_STRING ="TrxId "+ trx.getId()+" status "+trx.getStatus();
        String result = builder.apply(resultMapper);

        Assertions.assertEquals(EXPECTED_STRING, result);

    }
}
