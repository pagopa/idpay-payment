package it.gov.pagopa.payment.dto.idpaycode;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthPaymentIdpayCodeDTO extends AuthPaymentDTO {
  private String secondFactor;

}
