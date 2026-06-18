package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static it.gov.pagopa.payment.utils.Utilities.sanitizeForLog;

@Service
@Slf4j
public class TransactionInProgressServiceImpl implements TransactionInProgressService {
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TrxCodeGenUtil trxCodeGenUtil;
    private final TransactionNotifierService transactionNotifierService;
    private final AppConfigurationProperties.ExtendedTransactions appConfigurationProperties;
    private final TransactionSynchronizer transactionSynchronizer;

    public TransactionInProgressServiceImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            TrxCodeGenUtil trxCodeGenUtil,
            TransactionNotifierService transactionNotifierService,
            AppConfigurationProperties.ExtendedTransactions appConfigurationProperties,
            TransactionSynchronizer transactionSynchronizer) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.trxCodeGenUtil = trxCodeGenUtil;
        this.transactionNotifierService = transactionNotifierService;
        this.appConfigurationProperties = appConfigurationProperties;
        this.transactionSynchronizer = transactionSynchronizer;
    }

    @Override
    public void generateTrxCodeAndSave(TransactionInProgress trx, String flowName) {
        long retry = 1;
        String trxCode;
        while(true){
            trxCode = trxCodeGenUtil.get();
            if(transactionInProgressRepository.createIfExists(trx, trxCode).getUpsertedId()
                != null){
                break;
            }
            log.info(
                    "[{}] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
                    flowName,
                    retry);
        }
        trx.setTrxCode(trxCode);

        Transaction transaction = new Transaction();
        transactionSynchronizer.sync(trx, transaction);
        transactionRepository.save(transaction);
    }

    @Override
    public long findAndUpdateExpiredTransactionsStatus(String initiativeId) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            log.info("[BATCH_EXPIRED_VOUCHER] Starting expiration update for initiative: {}", sanitizeForLog(initiativeId));
            int updatedRows = transactionRepository.updateStatusForExpiredVoucherTransactions(initiativeId, now);

            log.info("[BATCH_EXPIRED_VOUCHER] Updated expired vouchers directly in DB: {}", updatedRows);
            return transactionInProgressRepository.updateStatusForExpiredVoucherTransactions(initiativeId);
        } catch (Exception e) {
            log.error("[UPDATE_EXPIRED_TRANSACTIONS_STATUS] Encountered an error during the update of the existing " +
                    "transactions for which trx status is expired, with message {}", e.getMessage(), e);
            throw new ExpirationStatusUpdateException(e.getMessage());
        }
    }

    @Override
    public long sendEventForStaleExpiredTransactions(String initiativeId) {
        Integer page = 0;
        long numberOfEvents = 0L;
        try {
            while (true) {
                List<TransactionInProgress> transactionInProgressList =
                        transactionInProgressRepository.findUnprocessedExpiredVoucherTransactions(
                                initiativeId, appConfigurationProperties.getSendExpiredSendBatchSize(), page);

                numberOfEvents = numberOfEvents + transactionInProgressList.size();
                transactionInProgressList.parallelStream().forEach(
                        transaction -> {
                            if (!transactionNotifierService.notify(
                                    transaction, transaction.getId())) {
                                log.error("[SEND_EVENT_FOR_STALE_EXPIRED_TRX] Unable to send trx with id {}",
                                        transaction.getId());
                                throw new ExpirationStatusUpdateException("Unable to send trx with id " +
                                        transaction.getId());
                            }
                        });

                if (transactionInProgressList.isEmpty() ||
                        transactionInProgressList.size() < appConfigurationProperties.getSendExpiredSendBatchSize()) {
                    log.info(
                            "[SEND_EVENT_FOR_STALE_EXPIRED_TRX] Successfully sent {} stale transactions in EXPIRED state" +
                                    "left unprocessed for recovery", numberOfEvents);
                    return numberOfEvents;
                }

                page++;

            }

        } catch (ExpirationStatusUpdateException expirationStatusUpdateException) {
            throw expirationStatusUpdateException;
        } catch (Exception e) {
            log.error("[SEND_EVENT_TRANSACTIONS_STATUS] Encountered an error during the process: {}",
                    e.getMessage(), e);
            throw new ExpirationStatusUpdateException(e.getMessage());
        }

    }

}