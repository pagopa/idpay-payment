package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionInProgress2BaseTransactionResponseMapperTest {

    private TransactionInProgress2BaseTransactionResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionInProgress2BaseTransactionResponseMapper(4300);
    }

    @Test
    void applyTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.CREATED)
                .reward(1000L)
                .build();
        BaseTransactionResponseDTO result = mapper.apply(trx);
        result.setTrxPngUrl("");
        result.setTrxTxtUrl("");

        Assertions.assertAll(() -> {
            assertionCommons(trx, result);
            Assertions.assertFalse(result.getSplitPayment());
        });
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void splitPaymentTrueTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.CREATED)
                .reward(200L)
                .build();
        BaseTransactionResponseDTO result = mapper.apply(trx);
        result.setTrxPngUrl("");
        result.setTrxTxtUrl("");

        Assertions.assertAll(() -> {
            assertionCommons(trx, result);
            Assertions.assertTrue(result.getSplitPayment());
        });
        TestUtils.checkNotNullFields(result);
    }

    private static void assertionCommons(TransactionInProgress trx, BaseTransactionResponseDTO result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx.getId(), result.getId());
        Assertions.assertEquals(trx.getTrxCode(), result.getTrxCode());
        Assertions.assertEquals(trx.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(trx.getMerchantId(),result.getMerchantId());
        Assertions.assertEquals(trx.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertEquals(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertEquals(trx.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(trx.getAmountCents(), result.getAmountCents());
        Assertions.assertEquals(trx.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertEquals(trx.getMcc(), result.getMcc());
        Assertions.assertEquals(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(trx.getStatus(), result.getStatus());
        Assertions.assertEquals(trx.getMerchantFiscalCode(), result.getMerchantFiscalCode());
        Assertions.assertEquals(trx.getVat(), result.getVat());
        Assertions.assertEquals(trx.getAmountCents()- trx.getReward(), result.getResidualAmountCents());
        Assertions.assertEquals(4300, result.getTrxExpirationMinutes());
    }

}
