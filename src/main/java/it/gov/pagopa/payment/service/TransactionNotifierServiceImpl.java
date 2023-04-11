package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class TransactionNotifierServiceImpl implements TransactionNotifierService {
    @Override
    public boolean notify(TransactionInProgress trx) {
        return true; //TODO notify into idpay-transactions kafka queue
    }
}
