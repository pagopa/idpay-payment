package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



class RelateUserResponseMapperTest {

    RelateUserResponseMapper mapper = new RelateUserResponseMapper();
    @Test
    void transactionMapper() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        RelateUserResponse result = mapper.transactionMapper(trx);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx.getId(), result.getId());
        Assertions.assertEquals(trx.getStatus(), result.getStatus());

        TestUtils.checkNotNullFields(result);
    }
}