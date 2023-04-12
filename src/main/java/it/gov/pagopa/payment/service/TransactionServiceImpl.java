package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionMapper;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionInProgress2TransactionMapper transactionInProgress2TransactionMapper;

    private final TransactionInProgressRepository transactionInProgressRepository;

    public TransactionServiceImpl(TransactionInProgress2TransactionMapper transactionInProgress2TransactionMapper, TransactionInProgressRepository transactionInProgressRepository) {
        this.transactionInProgress2TransactionMapper = transactionInProgress2TransactionMapper;
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public TransactionDTO getTransaction(String id, String userId) {
        TransactionInProgress transactionInProgress = transactionInProgressRepository.findById(id);

        if (transactionInProgress == null) {
            throw new ClientExceptionWithBody(
                    HttpStatus.NOT_FOUND,
                    "NOT FOUND",
                    "Cannot find Transaction with ID: [%s]".formatted(id));
        }

        if (!transactionInProgress.getUserId().equals(userId)){
            throw new ClientExceptionWithBody(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "No permission"); //TODO refactor error message
        }

        return transactionInProgress2TransactionMapper.apply(transactionInProgress);
    }
}
