package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionInProgress2TransactionMapperTest {
    private TransactionInProgress2TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionInProgress2TransactionMapper();
    }

    @Test
    void applyTest() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
        TransactionDTO result = mapper.apply(trx);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trx.getId(), result.getId());
        Assertions.assertEquals(trx.getTrxCode(), result.getTrxCode());
        Assertions.assertEquals(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertEquals(trx.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(trx.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(trx.getTrxChargeDate(), result.getTrxChargeDate());
        Assertions.assertEquals(trx.getAuthDate(), result.getAuthDate());
        Assertions.assertEquals(trx.getElaborationDateTime(), result.getElaborationDateTime());
        Assertions.assertEquals(trx.getHpan(), result.getHpan());
        Assertions.assertEquals(trx.getOperationType(), result.getOperationType());
        Assertions.assertEquals(trx.getOperationTypeTranscoded(), result.getOperationTypeTranscoded());
        Assertions.assertEquals(trx.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertEquals(trx.getCorrelationId(), result.getCorrelationId());
        Assertions.assertEquals(trx.getAmountCents(), result.getAmountCents());
        Assertions.assertEquals(trx.getEffectiveAmount(), result.getEffectiveAmount());
        Assertions.assertEquals(trx.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertEquals(trx.getMcc(), result.getMcc());
        Assertions.assertEquals(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(trx.getMerchantId(), result.getMerchantId());
        Assertions.assertEquals(trx.getSenderCode(), result.getSenderCode());
        Assertions.assertEquals(trx.getMerchantFiscalCode(), result.getMerchantFiscalCode());
        Assertions.assertEquals(trx.getVat(), result.getVat());
        Assertions.assertEquals(trx.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(trx.getStatus(), result.getStatus());
        Assertions.assertEquals(trx.getCallbackUrl(), result.getCallbackUrl());
    }
}
