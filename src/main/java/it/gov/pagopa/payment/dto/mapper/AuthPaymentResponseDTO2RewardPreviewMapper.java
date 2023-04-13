package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.RewardPreview;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentResponseDTO2RewardPreviewMapper
    implements Function<AuthPaymentResponseDTO, RewardPreview> {

  @Override
  public RewardPreview apply(AuthPaymentResponseDTO authPaymentResponseDTO) {
    return RewardPreview.builder()
        .rejectionReasons(authPaymentResponseDTO.getRejectionReasons())
        .status(authPaymentResponseDTO.getStatus())
        .build();
  }
}
