package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.MerchantDetailDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
      MerchantDetailDTO merchantDetailDTO = MerchantDetailDTOFaker.mockInstance(1);
    OffsetDateTime now = OffsetDateTime.now();
    TransactionInProgress result =
        mapper.apply(
            transactionCreationRequest, "CHANNEL", "MERCHANTID", "ACQUIRERID", merchantDetailDTO, "IDTRXISSUER");

    assertResponse(transactionCreationRequest, now, merchantDetailDTO, result);
  }

  void assertResponse(TransactionCreationRequest transactionCreationRequest, OffsetDateTime now, MerchantDetailDTO merchantDetailDTO, TransactionInProgress result){
      Assertions.assertNotNull(result);
      Assertions.assertNotNull(result.getId());
      Assertions.assertNotNull(result.getCorrelationId());
      Assertions.assertEquals(
              transactionCreationRequest.getInitiativeId(), result.getInitiativeId());
      Assertions.assertEquals(
              transactionCreationRequest.getAmountCents(), result.getAmountCents());
      Assertions.assertEquals(
              transactionCreationRequest.getAmountCents(),
              result.getEffectiveAmountCents());
      Assertions.assertEquals(
              "IDTRXISSUER", result.getIdTrxIssuer());
      Assertions.assertEquals(transactionCreationRequest.getMcc(), result.getMcc());
      Assertions.assertEquals(
              merchantDetailDTO.getFiscalCode(), result.getMerchantFiscalCode());
      Assertions.assertEquals(merchantDetailDTO.getVatNumber(), result.getVat());
      Assertions.assertFalse(result.getTrxDate().isBefore(now));
      Assertions.assertFalse(result.getTrxDate().isAfter(OffsetDateTime.now()));
      Assertions.assertFalse(result.getUpdateDate().isBefore(now.toLocalDateTime()));
      Assertions.assertFalse(result.getUpdateDate().isAfter(LocalDateTime.now()));
      Assertions.assertEquals(SyncTrxStatus.CREATED, result.getStatus());
      Assertions.assertEquals(
              PaymentConstants.OPERATION_TYPE_CHARGE, result.getOperationType());
      Assertions.assertEquals(OperationType.CHARGE, result.getOperationTypeTranscoded());
      Assertions.assertEquals("CHANNEL", result.getChannel());
      Assertions.assertEquals("MERCHANTID", result.getMerchantId());
      Assertions.assertEquals("ACQUIRERID", result.getAcquirerId());
      Assertions.assertEquals(transactionCreationRequest.getIdTrxAcquirer(), result.getIdTrxAcquirer());
      Assertions.assertEquals(merchantDetailDTO.getInitiativeName(), result.getInitiativeName());
      Assertions.assertEquals(merchantDetailDTO.getBusinessName(), result.getBusinessName());
  }

}
