package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import org.springframework.stereotype.Service;

@Service
public class RelateUserResponsePerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<RelateUserResponse> {
    @Override
    public String apply(RelateUserResponse relateUserResponse) {
        return "TrxId %s status %s".formatted(relateUserResponse.getId(), relateUserResponse.getStatus());
    }
}
