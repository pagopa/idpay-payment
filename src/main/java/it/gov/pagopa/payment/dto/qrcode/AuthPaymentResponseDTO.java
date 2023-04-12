package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.dto.Reward;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentResponseDTO {

  String transactionId;
  String initiativeId;
  String userId;
  String status;
  Reward reward;
  List<String> rejectionReasons;

}
