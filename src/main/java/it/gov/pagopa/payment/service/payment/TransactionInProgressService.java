package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionInProgressService {
    void generateTrxCodeAndSave(TransactionInProgress trx, String flowName);

    /**
     * Method that updates status to EXPIRED for transactions with extendedInitiative that either
     * has a trxEndDate or initiativeEndDate before the current date
     * @param initiativeId used to execute checks for a specific initiative
     * @return number of processed records
     */
    long findAndUpdateExpiredTransactionsStatus(String initiativeId);

    /**
     * Sends all expired transactions remained within the collection for more than the configured threshold time
     * @param initiativeId used to execute checks for a specific initiative
     * @return number of processed records
     */
    long sendEventForStaleExpiredTransactions(String initiativeId);
}
