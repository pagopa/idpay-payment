package it.gov.pagopa.payment.service;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
@Slf4j
public class AuthorizationRequestTimeoutServiceImpl implements AuthorizationRequestTimeoutService {
    private final TransactionInProgressRepository transactionInProgressRepository;

    public AuthorizationRequestTimeoutServiceImpl(TransactionInProgressRepository transactionInProgressRepository) {
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public void execute(Message<String> message) {
        long startTime = System.currentTimeMillis();
        String header = (String) message.getHeaders().get(PaymentConstants.MESSAGE_TOPIC);
        if (PaymentConstants.TIMEOUT_PAYMENT.equals(header)) {
            String trxId = message.getPayload();
            log.info("[TIMEOUT_PAYMENT] Start processing transaction with id {}", trxId);
            UpdateResult result = transactionInProgressRepository.updateTrxPostTimeout(trxId);
            if (result.getModifiedCount() != 0) {
                performanceLog(startTime,"Authorization request has expired for transaction with id %s".formatted(trxId));
            } else {
                performanceLog(startTime, "Authorization completed in time for transaction with id %s".formatted(trxId) );
            }
        } else {
            performanceLog(startTime, "Unhandled MESSAGE_TOPIC header: %s".formatted(header));
        }
    }

    private void performanceLog(long startTime, String message){
        PerformanceLogger.log(
                PaymentConstants.TIMEOUT_PAYMENT,
                startTime,
                message);
    }
}
