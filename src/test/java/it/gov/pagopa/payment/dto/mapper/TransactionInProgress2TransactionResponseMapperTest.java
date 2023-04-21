package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionInProgress2TransactionResponseMapperTest {

  private TransactionInProgress2TransactionResponseMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionInProgress2TransactionResponseMapper();
  }

  @Test
  void applyTest() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    TransactionResponse result = mapper.apply(trx);

    Assertions.assertAll(() -> {
      Assertions.assertNotNull(result);
      Assertions.assertEquals(trx.getInitiativeId(), result.getInitiativeId());
      Assertions.assertEquals(trx.getAcquirerId(), result.getAcquirerId());
      Assertions.assertEquals(trx.getAmountCents(), result.getAmountCents());
      Assertions.assertEquals(trx.getAmountCurrency(), result.getAmountCurrency());
      Assertions.assertEquals(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
      Assertions.assertEquals(trx.getIdTrxIssuer(), result.getIdTrxIssuer());
      Assertions.assertEquals(trx.getMcc(), result.getMcc());
      Assertions.assertEquals(trx.getTrxDate(), result.getTrxDate());
      Assertions.assertEquals(trx.getTrxCode(), result.getTrxCode());
      Assertions.assertEquals(trx.getStatus(), result.getStatus());
      Assertions.assertEquals(trx.getMerchantFiscalCode(), result.getMerchantFiscalCode());
      Assertions.assertEquals(trx.getVat(), result.getVat());
    });

    TestUtils.checkNotNullFields(result);
  }
}
