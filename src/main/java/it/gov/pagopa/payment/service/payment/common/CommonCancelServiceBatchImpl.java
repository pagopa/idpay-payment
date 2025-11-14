package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("commonCancelBatch")
public class CommonCancelServiceBatchImpl {

  private final CommonCancelServiceImpl commonCancelServiceImpl;
  private final TransactionInProgressRepository repository;

  public CommonCancelServiceBatchImpl(CommonCancelServiceImpl commonCancelServiceImpl, TransactionInProgressRepository repository) {
    this.commonCancelServiceImpl = commonCancelServiceImpl;
    this.repository = repository;
  }

  public void rejectPendingTransactions() {
    List<TransactionInProgress> transactions;
    int pageSize = 100;
    do {
      transactions = repository.findPendingTransactions(pageSize);
      log.info("[CANCEL_AUTHORIZED_TRANSACTIONS] Transactions to cancel: {} / {}", transactions.size(), pageSize);
      transactions.forEach(transaction ->
          commonCancelServiceImpl.cancelTransaction(
              transaction.getId(),
              transaction.getMerchantId(),
              transaction.getAcquirerId(),
              transaction.getPointOfSaleId()));
    } while (!transactions.isEmpty());
  }

}
