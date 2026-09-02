package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateTransactionsStatusRequest(
        @NotEmpty List<@NotEmpty String> transactionIds,
        @NotNull SyncTrxStatus status
) {
}

