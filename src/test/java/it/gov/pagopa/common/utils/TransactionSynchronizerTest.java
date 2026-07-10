package it.gov.pagopa.common.utils;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSynchronizerTest {

    private final TransactionSynchronizer synchronizer =
            new TransactionSynchronizer();

    @Test
    void shouldDoNothingWhenSourceIsNull() {
        Transaction target = new Transaction();

        synchronizer.sync(null, target);

        assertNull(target.getId());
    }

    @Test
    void shouldDoNothingWhenTargetIsNull() {
        TransactionInProgress source = new TransactionInProgress();

        assertDoesNotThrow(() ->
                synchronizer.sync(source, null));
    }

    @Test
    void shouldSynchronizeAllFields() {

        OffsetDateTime trxDate = OffsetDateTime.now();
        OffsetDateTime trxChargeDate = trxDate.plusMinutes(1);
        OffsetDateTime trxEndDate = trxDate.plusMinutes(2);

        LocalDateTime elaborationDate = LocalDateTime.now();
        LocalDateTime updateDate = LocalDateTime.now().plusMinutes(1);

        InvoiceData invoiceData = new InvoiceData();
        invoiceData.setFilename("invoice.pdf");
        invoiceData.setDocNumber("INV-001");

        InvoiceData creditNoteData = new InvoiceData();
        creditNoteData.setFilename("credit.pdf");
        creditNoteData.setDocNumber("CRN-001");

        TransactionInProgress source = new TransactionInProgress();

        source.setId("id");
        source.setTrxCode("trxCode");
        source.setIdTrxAcquirer("acquirer");
        source.setIdTrxIssuer("issuer");
        source.setCorrelationId("corr");

        source.setTrxDate(trxDate);
        source.setTrxChargeDate(trxChargeDate);
        source.setTrxEndDate(trxEndDate);

        source.setElaborationDateTime(elaborationDate);
        source.setUpdateDate(updateDate);

        source.setOperationType("PAYMENT");
        source.setOperationTypeTranscoded(OperationType.CHARGE);

        source.setStatus(SyncTrxStatus.AUTHORIZED);

        source.setAmountCents(100L);
        source.setEffectiveAmountCents(90L);
        source.setVoucherAmountCents(10L);
        source.setRewardCents(5L);

        source.setAmountCurrency("EUR");

        source.setMerchantId("merchant");
        source.setMerchantFiscalCode("fiscalCode");
        source.setBusinessName("business");
        source.setFranchiseName("franchise");
        source.setVat("vat");

        source.setChannel("APP");

        source.setInitiativeId("initiative");
        source.setInitiativeName("initiativeName");
        source.setInitiatives(List.of("INIT1"));

        source.setUserId("user");

        source.setAcquirerId("acquirerId");

        source.setPointOfSaleId("pos");
        source.setPointOfSaleType("PHYSICAL");

        source.setFamilyId("family");

        source.setRewards(Map.of());

        source.setRejectionReasons(List.of("REASON"));

        source.setInitiativeRejectionReasons(
                Map.of("INIT1", List.of("R1")));

        source.setAdditionalProperties(
                Map.of("key", "value"));

        source.setCounterVersion(1L);

        source.setMcc("5411");

        source.setExtendedAuthorization(true);

        source.setInvoiceData(invoiceData);
        source.setCreditNoteData(creditNoteData);

        Transaction target = new Transaction();

        synchronizer.sync(source, target);

        assertEquals(source.getId(), target.getId());
        assertEquals(source.getTrxCode(), target.getTrxCode());
        assertEquals(source.getIdTrxAcquirer(), target.getIdTrxAcquirer());
        assertEquals(source.getIdTrxIssuer(), target.getIdTrxIssuer());
        assertEquals(source.getCorrelationId(), target.getCorrelationId());

        assertEquals(source.getTrxDate(), target.getTrxDate());
        assertEquals(source.getTrxChargeDate(), target.getTrxChargeDate());
        assertEquals(source.getTrxEndDate(), target.getTrxEndDate());
    }

}