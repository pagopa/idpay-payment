package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;

public interface TransactionService {
    SyncTrxStatusDTO getTransaction(String id, String userId);
}
