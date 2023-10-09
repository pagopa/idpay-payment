package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import org.springframework.stereotype.Service;

@Service
public class TransactionBarCodeResponsePerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<TransactionBarCodeResponse> {
    @Override
    public String apply(TransactionBarCodeResponse transactionBarCodeResponse) {
        return "TrxId %s status %s".formatted(transactionBarCodeResponse.getId(), transactionBarCodeResponse.getStatus());
    }
}
