package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.entity.Transaction;

public final class StoragePathUtils {

    private static final String INVOICE_FOLDER = "invoice";
    private static final String CREDIT_NOTE_FOLDER = "creditNote";

    private StoragePathUtils() {
    }

    public static String buildInvoicePath(Transaction transaction, String filename) {
        return buildPath(transaction, INVOICE_FOLDER, filename);
    }

    public static String buildCreditNotePath(Transaction transaction, String filename) {
        return buildPath(transaction, CREDIT_NOTE_FOLDER, filename);
    }

    private static String buildPath(Transaction transaction, String folderName, String filename) {
        return String.format(
                "invoices/%s/merchant/%s/pos/%s/transaction/%s/%s/%s",
                transaction.getInitiativeId(),
                transaction.getMerchantId(),
                transaction.getPointOfSaleId(),
                transaction.getId(),
                folderName,
                filename
        );
    }
}

