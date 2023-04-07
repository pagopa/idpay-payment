package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.enums.Status;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionInProgress2TransactionCreatedMapperTest {

  private TransactionInProgress2TransactionCreatedMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionInProgress2TransactionCreatedMapper();
  }

  @Test
  void applyTest() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
    TransactionCreated result = mapper.apply(trx);

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trx.getInitiativeId(), result.getInitiativeId());
    Assertions.assertEquals(trx.getAcquirerCode(), result.getAcquirerCode());
    Assertions.assertEquals(trx.getAcquirerId(), result.getAcquirerId());
    Assertions.assertEquals(trx.getAmount(), result.getAmount());
    Assertions.assertEquals(trx.getAmountCurrency(), result.getAmountCurrency());
    Assertions.assertEquals(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
    Assertions.assertEquals(trx.getIdTrxIssuer(), result.getIdTrxIssuer());
    Assertions.assertEquals(trx.getMcc(), result.getMcc());
    Assertions.assertEquals(trx.getSenderCode(), result.getSenderCode());
    Assertions.assertEquals(trx.getTrxDate(), result.getTrxDate());
    Assertions.assertEquals(trx.getTrxCode(), result.getTrxCode());
  }
}
