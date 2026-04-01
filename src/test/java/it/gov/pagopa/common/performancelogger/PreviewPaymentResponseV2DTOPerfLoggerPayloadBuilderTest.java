package it.gov.pagopa.common.performancelogger;

import it.gov.pagopa.payment.dto.PreviewPaymentResponseV2DTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilderTest {

    @Test
    void testApply() {
        PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder builder = new PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder();

        PreviewPaymentResponseV2DTO previewPaymentResponseV2DTO = PreviewPaymentResponseV2DTO.builder()
                .trxCode("trxCode")
                .status(SyncTrxStatus.AUTHORIZED)
                .build();
        String expected = "TrxCode %s status %s"
                .formatted(previewPaymentResponseV2DTO.getTrxCode(), previewPaymentResponseV2DTO.getStatus());

        String result = builder.apply(previewPaymentResponseV2DTO);

        Assertions.assertEquals(expected, result);
    }
}
