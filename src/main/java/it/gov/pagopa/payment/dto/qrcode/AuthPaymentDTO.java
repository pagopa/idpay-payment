package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.dto.Reward;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentDTO {

  String id;
  String trxCode;
  String initiativeId;
  String status;
  Reward reward;
  List<String> rejectReasons;

}
