package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

  private final TransactionInProgressRepository transactionInProgressRepository;

  public TransactionServiceImpl(TransactionInProgressRepository transactionInProgressRepository) {
    this.transactionInProgressRepository = transactionInProgressRepository;
  }

  @Override
  public TransactionInProgress getTransaction(String id, String userId) {
    TransactionInProgress transactionInProgress = transactionInProgressRepository.findById(id)
        .orElse(null);

    if (transactionInProgress == null) {
      throw new ClientExceptionNoBody(
          HttpStatus.NOT_FOUND,
          "NOT FOUND");
    }

    if (!userId.equals(transactionInProgress.getUserId())) {
      throw new ClientExceptionNoBody(
          HttpStatus.FORBIDDEN,
          "FORBIDDEN");
    }

    return transactionInProgress;
  }

  @Override
  public SyncTrxStatus getStatusTransaction(String transactionId, String merchantId) {
    TransactionInProgress transactionInProgress= transactionInProgressRepository.
            findById(transactionId)
            .filter(t-> t.getMerchantId().equals(merchantId))
            .orElseThrow(()-> new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

    return transactionInProgress.getStatus();

  }
}
