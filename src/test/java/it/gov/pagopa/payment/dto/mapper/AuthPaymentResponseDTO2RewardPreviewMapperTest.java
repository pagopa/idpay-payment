package it.gov.pagopa.payment.dto.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.RewardPreview;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthPaymentResponseDTO2RewardPreviewMapperTest {

  private AuthPaymentResponseDTO2RewardPreviewMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AuthPaymentResponseDTO2RewardPreviewMapper();
  }

  @Test
  void apply() {

    AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
    authPaymentResponseDTO.setRejectionReasons(List.of());

    RewardPreview result = mapper.apply(authPaymentResponseDTO);

    Assertions.assertAll(() -> {
      Assertions.assertNotNull(result);
      TestUtils.checkNotNullFields(result);
      Assertions.assertEquals(authPaymentResponseDTO.getStatus(), authPaymentResponseDTO.getStatus());
      Assertions.assertEquals(authPaymentResponseDTO.getRejectionReasons(), result.getRejectionReasons());
    });
  }
}
