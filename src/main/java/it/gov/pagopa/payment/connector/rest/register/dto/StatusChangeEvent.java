package it.gov.pagopa.payment.connector.rest.register.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusChangeEvent {
    private String username;
    private String role;
    private String motivation;
    private LocalDateTime updateDate;
    private ProductStatus currentStatus;
    private ProductStatus targetStatus;
}