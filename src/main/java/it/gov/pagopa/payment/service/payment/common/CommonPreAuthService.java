package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface CommonPreAuthService {
    TransactionInProgress relateUser(TransactionInProgress trx, String userId);
    AuthPaymentDTO previewPayment(TransactionInProgress trx);
    void auditLogUserRelate(TransactionInProgress trx);
}
