package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.Status;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionCreationRequest2TransactionInProgressMapperTest {

  private TransactionCreationRequest2TransactionInProgressMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionCreationRequest2TransactionInProgressMapper();
  }

  @Test
  void applyTest() {

    TransactionCreationRequest transactionCreationRequest =
        TransactionCreationRequestFaker.mockInstance(1);
    TransactionInProgress result = mapper.apply(transactionCreationRequest);

    Assertions.assertNotNull(result);
    Assertions.assertEquals(transactionCreationRequest.getInitiativeId(), result.getInitiativeId());
    Assertions.assertEquals(transactionCreationRequest.getAcquirerCode(), result.getAcquirerCode());
    Assertions.assertEquals(transactionCreationRequest.getAcquirerId(), result.getAcquirerId());
    Assertions.assertEquals(transactionCreationRequest.getAmount(), result.getAmount());
    Assertions.assertEquals(
        transactionCreationRequest.getAmountCurrency(), result.getAmountCurrency());
    Assertions.assertEquals(transactionCreationRequest.getCallbackUrl(), result.getCallbackUrl());
    Assertions.assertEquals(
        transactionCreationRequest.getIdTrxAcquirer(), result.getIdTrxAcquirer());
    Assertions.assertEquals(transactionCreationRequest.getIdTrxIssuer(), result.getIdTrxIssuer());
    Assertions.assertEquals(transactionCreationRequest.getMcc(), result.getMcc());
    Assertions.assertEquals(
        transactionCreationRequest.getMerchantFiscalCode(), result.getMerchantFiscalCode());
    Assertions.assertEquals(transactionCreationRequest.getSenderCode(), result.getSenderCode());
    Assertions.assertEquals(transactionCreationRequest.getVat(), result.getVat());
    Assertions.assertEquals(transactionCreationRequest.getTrxDate(), result.getTrxDate());
    Assertions.assertEquals(Status.CREATED, result.getStatus());
  }
}
