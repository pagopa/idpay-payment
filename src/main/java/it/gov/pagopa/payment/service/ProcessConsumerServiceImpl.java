package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
@Service
@Slf4j
public class ProcessConsumerServiceImpl implements ProcessConsumerService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final AuditUtilities auditUtilities;

    public ProcessConsumerServiceImpl(TransactionInProgressRepository transactionInProgressRepository, AuditUtilities auditUtilities) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.auditUtilities = auditUtilities;
    }

    @PerformanceLog("DELETE_TRANSACTIONS_IN_PROGRESS")
    @Override
    public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {

        if (("DELETE_INITIATIVE").equals(queueCommandOperationDTO.getOperationType())) {
            List<TransactionInProgress> deletedTrx = transactionInProgressRepository
                    .deleteByInitiativeId(queueCommandOperationDTO.getEntityId())
                    .orElse(Collections.emptyList());
            List<String> usersId = deletedTrx.stream().map(TransactionInProgress::getUserId).distinct().toList();

            log.info("[DELETE OPERATION] Deleted {} transactions in progress for initiative: {}", deletedTrx.size(),
                    queueCommandOperationDTO.getEntityId());

            usersId.forEach(userId -> auditUtilities.logDeleteTransactions(userId, queueCommandOperationDTO.getEntityId()));
        }
    }
}
