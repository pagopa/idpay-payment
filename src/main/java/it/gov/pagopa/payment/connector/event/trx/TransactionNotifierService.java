package it.gov.pagopa.payment.connector.event.trx;

import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.messaging.Message;

public interface TransactionNotifierService {
    boolean notify(TransactionInProgress trx, String key);
    Message<TransactionOutcomeDTO> buildMessage(TransactionInProgress trx, String key);
}
