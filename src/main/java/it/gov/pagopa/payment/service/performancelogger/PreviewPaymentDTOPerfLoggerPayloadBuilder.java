package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import org.springframework.stereotype.Service;

@Service
public class PreviewPaymentDTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<PreviewPaymentDTO> {
    @Override
    public String apply(PreviewPaymentDTO previewPaymentDTO) {
        return "TrxCode %s status %s".formatted(previewPaymentDTO.getTrxCode(), previewPaymentDTO.getStatus());
    }
}
