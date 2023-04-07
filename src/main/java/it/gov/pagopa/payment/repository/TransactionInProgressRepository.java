package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionInProgressRepository {
  TransactionInProgress createIfNotExists(TransactionInProgress trx);
}
