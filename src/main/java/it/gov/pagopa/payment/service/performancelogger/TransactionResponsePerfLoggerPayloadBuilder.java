package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.stereotype.Service;

@Service
public class TransactionResponsePerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<TransactionResponse> {
    @Override
    public String apply(TransactionResponse transactionResponse) {
        return "TrxId %s status %s".formatted(transactionResponse.getId(), transactionResponse.getStatus());
    }
}
