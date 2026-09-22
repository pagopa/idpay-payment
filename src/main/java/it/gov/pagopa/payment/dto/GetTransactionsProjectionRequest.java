package it.gov.pagopa.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record GetTransactionsProjectionRequest(
        @NotEmpty
        @Size(max = 100)
        Set<@NotBlank String> transactionIds
) {
}

