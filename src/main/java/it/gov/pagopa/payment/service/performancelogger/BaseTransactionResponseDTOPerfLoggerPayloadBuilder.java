package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class BaseTransactionResponseDTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<BaseTransactionResponseDTO> {
    @Override
    public String apply(BaseTransactionResponseDTO relate) {
        return "TrxId %s status %s".formatted(relate.getId(), relate.getStatus());
    }
}
