package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.stereotype.Service;

@Service
public class BaseTransactionResponseDTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<TransactionResponse> {
    @Override
    public String apply(TransactionResponse relate) {
        return "TrxId %s status %s".formatted(relate.getId(), relate.getStatus());
    }
}
