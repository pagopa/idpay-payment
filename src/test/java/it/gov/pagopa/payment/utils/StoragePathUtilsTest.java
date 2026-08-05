package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.entity.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePathUtilsTest {

    @Test
    void buildInvoicePath_shouldUseTransactionInitiativeId() {
        Transaction transaction = Transaction.builder()
                .id("TRX1")
                .initiativeId("INITIATIVE1")
                .merchantId("MERCHANT1")
                .pointOfSaleId("POS1")
                .build();

        String path = StoragePathUtils.buildInvoicePath(transaction, "invoice.pdf");

        assertEquals(
                "invoices/INITIATIVE1/merchant/MERCHANT1/pos/POS1/transaction/TRX1/invoice/invoice.pdf",
                path
        );
    }

    @Test
    void buildCreditNotePath_shouldUseTransactionInitiativeId() {
        Transaction transaction = Transaction.builder()
                .id("TRX1")
                .initiativeId("INITIATIVE1")
                .merchantId("MERCHANT1")
                .pointOfSaleId("POS1")
                .build();

        String path = StoragePathUtils.buildCreditNotePath(transaction, "credit-note.pdf");

        assertEquals(
                "invoices/INITIATIVE1/merchant/MERCHANT1/pos/POS1/transaction/TRX1/creditNote/credit-note.pdf",
                path
        );
    }
}

