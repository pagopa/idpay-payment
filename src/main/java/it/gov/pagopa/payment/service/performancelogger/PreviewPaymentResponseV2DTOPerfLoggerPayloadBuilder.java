package it.gov.pagopa.payment.service.performancelogger;

import it.gov.pagopa.common.performancelogger.PerformanceLoggerPayloadBuilder;
import it.gov.pagopa.payment.dto.PreviewPaymentResponseV2DTO;
import org.springframework.stereotype.Service;

@Service
public class PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder implements PerformanceLoggerPayloadBuilder<PreviewPaymentResponseV2DTO> {
    @Override
    public String apply(PreviewPaymentResponseV2DTO previewPaymentResponseV2DTO) {
        return "TrxCode %s status %s".formatted(previewPaymentResponseV2DTO.getTrxCode(), previewPaymentResponseV2DTO.getStatus());
    }
}
