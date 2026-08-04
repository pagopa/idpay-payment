package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.entity.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePathUtilsTest {

    @Test
    void buildInvoicePath_shouldUseElettrodomesticiInitiative() {
        Transaction transaction = Transaction.builder()
                .id("TRX1")
                .merchantId("MERCHANT1")
                .pointOfSaleId("POS1")
                .build();

        String path = StoragePathUtils.buildInvoicePath(transaction, "invoice.pdf");

        assertEquals(
                "invoices/elettrodomestici/merchant/MERCHANT1/pos/POS1/transaction/TRX1/invoice/invoice.pdf",
                path
        );
    }

    @Test
    void buildCreditNotePath_shouldUseElettrodomesticiInitiative() {
        Transaction transaction = Transaction.builder()
                .id("TRX1")
                .merchantId("MERCHANT1")
                .pointOfSaleId("POS1")
                .build();

        String path = StoragePathUtils.buildCreditNotePath(transaction, "credit-note.pdf");

        assertEquals(
                "invoices/elettrodomestici/merchant/MERCHANT1/pos/POS1/transaction/TRX1/creditNote/credit-note.pdf",
                path
        );
    }
}

