package it.gov.pagopa.payment.dto.mapper.barcode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionBarCodeInProgress2TransactionResponseMapperTest {
    private TransactionBarCodeInProgress2TransactionResponseMapper mapper;



    @BeforeEach
    void setUp() {
        mapper = new TransactionBarCodeInProgress2TransactionResponseMapper(5);
    }

    @Test
    void applyTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        TransactionBarCodeResponse result = mapper.apply(trx);

        assertionCommons(trx, result);

        TestUtils.checkNotNullFields(result);
    }
    private static void assertionCommons(TransactionInProgress trx, TransactionBarCodeResponse result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(trx.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(trx.getTrxCode(), result.getTrxCode());
        Assertions.assertEquals(trx.getStatus(), result.getStatus());
        Assertions.assertEquals(trx.getId(), result.getId());
    }
}