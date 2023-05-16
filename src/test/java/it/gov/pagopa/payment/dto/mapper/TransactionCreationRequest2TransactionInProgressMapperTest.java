package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.utils.Utils;
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
    TransactionInProgress result =
        mapper.apply(
            transactionCreationRequest, "CHANNEL", "MERCHANTID", "ACQUIRERID", "IDTRXACQUIRER");

    Assertions.assertAll(
        () -> {
          Assertions.assertNotNull(result);
          Assertions.assertNotNull(result.getId());
          Assertions.assertNotNull(result.getCorrelationId());
          Assertions.assertEquals(
              transactionCreationRequest.getInitiativeId(), result.getInitiativeId());
          Assertions.assertEquals(
              transactionCreationRequest.getAmountCents(), result.getAmountCents());
          Assertions.assertEquals(
              Utils.centsToEuro(transactionCreationRequest.getAmountCents()),
              result.getEffectiveAmount());
          Assertions.assertEquals(
              transactionCreationRequest.getIdTrxIssuer(), result.getIdTrxIssuer());
          Assertions.assertEquals(transactionCreationRequest.getMcc(), result.getMcc());
          Assertions.assertEquals(
              transactionCreationRequest.getMerchantFiscalCode(), result.getMerchantFiscalCode());
          Assertions.assertEquals(transactionCreationRequest.getVat(), result.getVat());
          Assertions.assertEquals(transactionCreationRequest.getTrxDate(), result.getTrxDate());
          Assertions.assertEquals(
              transactionCreationRequest.getTrxDate(), result.getTrxChargeDate());
          Assertions.assertEquals(SyncTrxStatus.CREATED, result.getStatus());
          Assertions.assertEquals(
              PaymentConstants.OPERATION_TYPE_CHARGE, result.getOperationType());
          Assertions.assertEquals(OperationType.CHARGE, result.getOperationTypeTranscoded());
          Assertions.assertEquals("CHANNEL", result.getChannel());
          Assertions.assertEquals("MERCHANTID", result.getMerchantId());
          Assertions.assertEquals("ACQUIRERID", result.getAcquirerId());
          Assertions.assertEquals("IDTRXACQUIRER", result.getIdTrxAcquirer());
        });
  }
}
