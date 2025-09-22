package it.gov.pagopa.payment.dto.mapper.barcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionEnrichedResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

class TransactionBarCodeInProgress2TransactionEnrichedResponseMapperTest {
    private static final int AUTH_EXPIRATION_MINUTES = 5;
    private static final int AUTH_EXTENDED_EXPIRATION_MINUTES = 2880;

    private TransactionBarCodeInProgress2TransactionEnrichedResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionBarCodeInProgress2TransactionEnrichedResponseMapper(AUTH_EXPIRATION_MINUTES, AUTH_EXTENDED_EXPIRATION_MINUTES);
    }

    @Test
    void applyTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setExtendedAuthorization(false);
        TransactionBarCodeEnrichedResponse result = mapper.apply(trx);

        assertionCommons(trx, result);
        Assertions.assertEquals(CommonUtilities.minutesToSeconds(AUTH_EXPIRATION_MINUTES), result.getTrxExpirationSeconds());
        OffsetDateTime expectedEndDate = trx.getTrxDate().plusMinutes(AUTH_EXPIRATION_MINUTES);
        Assertions.assertEquals(expectedEndDate, result.getTrxEndDate());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void applyTest_extendedTransaction() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setExtendedAuthorization(true);
        TransactionBarCodeEnrichedResponse result = mapper.apply(trx);

        assertionCommons(trx, result);

        OffsetDateTime expectedEndTime = trx.getTrxDate().plusMinutes(AUTH_EXTENDED_EXPIRATION_MINUTES)
                .truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);


        Assertions.assertEquals(CommonUtilities.secondsBetween(trx.getTrxDate(), expectedEndTime), result.getTrxExpirationSeconds());
        Assertions.assertEquals(expectedEndTime, result.getTrxEndDate());

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