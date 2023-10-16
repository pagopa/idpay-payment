package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

  private final TransactionInProgressRepository transactionInProgressRepository;

  private final TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper;

  public TransactionServiceImpl(TransactionInProgressRepository transactionInProgressRepository, TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.transaction2statusMapper = transaction2statusMapper;
  }

  @Override
  public SyncTrxStatusDTO getTransaction(String id, String userId) {
    TransactionInProgress transactionInProgress = transactionInProgressRepository.findById(id)
        .orElse(null);

    if (transactionInProgress == null) {
      throw new NotFoundException("NOT FOUND", "Transaction not found");
    }

    if (!userId.equals(transactionInProgress.getUserId())) {
      throw new ForbiddenException("FORBIDDEN", "User does not exist");
    }

    return transaction2statusMapper.transactionInProgressMapper(transactionInProgress);
  }
}
