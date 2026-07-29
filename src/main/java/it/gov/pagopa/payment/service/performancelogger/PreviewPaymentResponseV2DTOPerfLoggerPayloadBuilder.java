package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.PreviewPaymentResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<PreviewPaymentResponseDTO> {
    @Override
    public String apply(PreviewPaymentResponseDTO previewPaymentResponseDTO) {
        return "TrxCode %s status %s".formatted(previewPaymentResponseDTO.getTrxCode(), previewPaymentResponseDTO.getStatus());
    }
}
