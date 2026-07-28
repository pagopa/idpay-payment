package it.gov.pagopa.payment.connector.event.trx;

import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.messaging.Message;

public interface TransactionNotifierService {
    boolean notify(Transaction trx, String key);
    Message<Transaction> buildMessage(Transaction transaction, String key);
}