package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.test.fakers.PreviewPaymentDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewPaymentDTOPerfLoggerPayloadBuilderTest {

    @Test
    void testApply() {
        PreviewPaymentDTOPerfLoggerPayloadBuilder builder = new PreviewPaymentDTOPerfLoggerPayloadBuilder();

        PreviewPaymentDTO previewPaymentDTO = PreviewPaymentDTOFaker.mockInstance();
        String APPLY_STRING = "TrxCode %s status %s"
                .formatted(previewPaymentDTO.getTrxCode(), previewPaymentDTO.getStatus());

        String result = builder.apply(previewPaymentDTO);

        Assertions.assertEquals(APPLY_STRING, result);

    }
}
