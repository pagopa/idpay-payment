package it.gov.pagopa.payment.dto.mapper.barcode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class TransactionBarCodeCreationRequest2TransactionInProgressMapperTest {
    private TransactionBarCodeCreationRequest2TransactionInProgressMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionBarCodeCreationRequest2TransactionInProgressMapper();
    }

    @Test
    void applyTest() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();
        OffsetDateTime now = OffsetDateTime.now();
        TransactionInProgress result =
                mapper.apply(
                        trxCreationReq, "CHANNEL", "USERID", "INITIATIVENAME");

        TestUtils.checkNotNullFields(result, "trxCode", "idTrxAcquirer", "trxChargeDate",
                "elaborationDateTime", "idTrxIssuer", "amountCents", "effectiveAmount", "amountCurrency",
                "mcc", "acquirerId", "merchantId", "merchantFiscalCode", "vat", "initiativeName", "businessName",
                "reward", "rejectionReasons", "rewards", "initiativeRejectionReasons");
        assertResponse(trxCreationReq, now, result);
    }
    void assertResponse(TransactionBarCodeCreationRequest trxCreationReq, OffsetDateTime now, TransactionInProgress result){
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertNotNull(result.getCorrelationId());
        Assertions.assertEquals(
                trxCreationReq.getInitiativeId(), result.getInitiativeId());
        Assertions.assertFalse(result.getTrxDate().isBefore(now));
        Assertions.assertFalse(result.getTrxDate().isAfter(OffsetDateTime.now()));
        Assertions.assertFalse(result.getUpdateDate().isBefore(now.toLocalDateTime()));
        Assertions.assertFalse(result.getUpdateDate().isAfter(LocalDateTime.now()));
        Assertions.assertEquals(SyncTrxStatus.CREATED, result.getStatus());
        Assertions.assertEquals(
                PaymentConstants.OPERATION_TYPE_CHARGE, result.getOperationType());
        Assertions.assertEquals(OperationType.CHARGE, result.getOperationTypeTranscoded());
        Assertions.assertEquals("CHANNEL", result.getChannel());
        Assertions.assertEquals("USERID", result.getUserId());
    }
}