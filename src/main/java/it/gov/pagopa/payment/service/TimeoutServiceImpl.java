package it.gov.pagopa.payment.service;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
@Slf4j
public class TimeoutServiceImpl implements TimeoutService{
    private final TransactionInProgressRepository transactionInProgressRepository;

    public TimeoutServiceImpl(TransactionInProgressRepository transactionInProgressRepository) {
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public void timeoutConsumer(Message<String> message) {
        long startTime = System.currentTimeMillis();
        if (PaymentConstants.TIMEOUT_PAYMENT.equals(message.getHeaders().get(PaymentConstants.MESSAGE_TOPIC))) {
            String trxId = message.getPayload();
            log.info("[TIMEOUT_PAYMENT] Start processing transaction with id %s".formatted(trxId));
            UpdateResult result = transactionInProgressRepository.updateTrxPostTimeout(trxId);
            if (result.getModifiedCount() != 0) {
                PerformanceLogger.log(PaymentConstants.TIMEOUT_PAYMENT, startTime,"Transaction with id %s updated in status REJECTED".formatted(trxId));
            } else {
                PerformanceLogger.log(PaymentConstants.TIMEOUT_PAYMENT, startTime, "Authorization completed in time for transaction with id %s".formatted(trxId));
            }
        } else {
            PerformanceLogger.log(PaymentConstants.TIMEOUT_PAYMENT, startTime,
                    "Skipping message for message topic invalid: %s".formatted(message.getHeaders().get(PaymentConstants.MESSAGE_TOPIC)));
        }
    }
}
