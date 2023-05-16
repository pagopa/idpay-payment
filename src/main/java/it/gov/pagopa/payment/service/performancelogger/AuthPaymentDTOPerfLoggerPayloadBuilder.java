package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentDTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<AuthPaymentDTO> {
    @Override
    public String apply(AuthPaymentDTO authPaymentDTO) {
        return "TrxId %s status %s".formatted(authPaymentDTO.getId(), authPaymentDTO.getStatus());
    }
}
