package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
@Slf4j
@SuppressWarnings("BusyWait")
public class ProcessConsumerServiceImpl implements ProcessConsumerService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final AuditUtilities auditUtilities;
    private static final String PAGINATION_KEY = "pagination";
    private static final String DELAY_KEY = "delay";

    public ProcessConsumerServiceImpl(TransactionInProgressRepository transactionInProgressRepository, AuditUtilities auditUtilities) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {

        if (("DELETE_INITIATIVE").equals(queueCommandOperationDTO.getOperationType())) {
            long startTime = System.currentTimeMillis();

            List<TransactionInProgress> deletedTrx = new ArrayList<>();
            List<TransactionInProgress> fetchedTrx;

            do {
                fetchedTrx = transactionInProgressRepository.deletePaged(queueCommandOperationDTO.getEntityId(),
                        Integer.parseInt(queueCommandOperationDTO.getAdditionalParams().get(PAGINATION_KEY)));
                deletedTrx.addAll(fetchedTrx);
                try{
                    Thread.sleep(Long.parseLong(queueCommandOperationDTO.getAdditionalParams().get(DELAY_KEY)));
                } catch (InterruptedException e){
                    log.error("An error has occurred while waiting {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } while (fetchedTrx.size() == (Integer.parseInt(queueCommandOperationDTO.getAdditionalParams().get(PAGINATION_KEY))));

            List<String> usersId = deletedTrx.stream().map(TransactionInProgress::getUserId).distinct().toList();

            log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: transaction_in_progress",
                    queueCommandOperationDTO.getEntityId());

            usersId.forEach(userId -> auditUtilities.logDeleteTransactions(userId, queueCommandOperationDTO.getEntityId()));
            log.info(
                    "[PERFORMANCE_LOG] [DELETE_INITIATIVE] Time occurred to perform business logic: {} ms",
                    System.currentTimeMillis() - startTime);
        }
    }
}
