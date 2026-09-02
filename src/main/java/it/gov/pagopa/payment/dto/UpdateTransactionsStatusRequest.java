package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTransactionsStatusRequest(
        @NotEmpty
        @Size(max = 100)
        List<@NotEmpty String> transactionIds,
        @NotNull SyncTrxStatus status
) {
}
