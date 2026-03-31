package it.gov.pagopa.payment.connector.rest.register.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeEvent {
    private String username;
    private String role;
    private String motivation;
    private Instant updateDate;
    private ProductStatus currentStatus;
    private ProductStatus targetStatus;
}