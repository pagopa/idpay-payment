package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.PreviewPaymentResponseDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewPaymentResponseDTOPerfLoggerPayloadBuilderTest {

    @Test
    void testApply() {
        PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder builder = new PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder();

        PreviewPaymentResponseDTO previewPaymentResponseDTO = PreviewPaymentResponseDTO.builder()
                .trxCode("trxCode")
                .status(SyncTrxStatus.AUTHORIZED)
                .build();
        String expected = "TrxCode %s status %s"
                .formatted(previewPaymentResponseDTO.getTrxCode(), previewPaymentResponseDTO.getStatus());

        String result = builder.apply(previewPaymentResponseDTO);

        Assertions.assertEquals(expected, result);
    }
}
