package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TransactionDTO;

public interface TransactionService {
    TransactionDTO getTransaction(String id, String userId);
}
