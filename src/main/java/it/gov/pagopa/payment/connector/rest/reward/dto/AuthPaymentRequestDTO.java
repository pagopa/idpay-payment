package it.gov.pagopa.payment.connector.rest.reward.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthPaymentRequestDTO extends PaymentRequestDTO {

    private long rewardCents;
}
