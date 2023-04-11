package it.gov.pagopa.payment.dto.qrcode;

import java.math.BigDecimal;
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
  BigDecimal reward;
  List<String> rejectReasons;

}
