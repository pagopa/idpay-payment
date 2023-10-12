package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionInProgress2TransactionResponseMapperTest {

    private TransactionInProgress2TransactionResponseMapper mapper;

    private static final String QRCODE_IMG_BASEURL = "QRCODE_IMG_BASEURL";
    private static final String QRCODE_TXT_BASEURL = "QRCODE_TXT_BASEURL";

    @BeforeEach
    void setUp() {
        mapper = new TransactionInProgress2TransactionResponseMapper(4300, QRCODE_IMG_BASEURL, QRCODE_TXT_BASEURL);
    }

    @Test
    void applyTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.CREATED)
                .reward(1000L)
                .build();
        TransactionResponse result = mapper.apply(trx);
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
        TransactionResponse result = mapper.apply(trx);
        result.setTrxPngUrl("");
        result.setTrxTxtUrl("");

        Assertions.assertAll(() -> {
            assertionCommons(trx, result);
            Assertions.assertTrue(result.getSplitPayment());
        });

        TestUtils.checkNotNullFields(result);
    }

    private static void assertionCommons(TransactionInProgress trx, TransactionResponse result) {
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
        Assertions.assertEquals(trx.getAmountCents()- trx.getReward(), result.getResidualAmountCents());
        Assertions.assertEquals(QRCODE_IMG_BASEURL.concat("?trxcode=%s".formatted(trx.getTrxCode())), result.getQrcodePngUrl());
        Assertions.assertEquals(QRCODE_TXT_BASEURL.concat("/%s".formatted(trx.getTrxCode())), result.getQrcodeTxtUrl());
        Assertions.assertEquals(4300, result.getTrxExpirationMinutes());
    }

}
