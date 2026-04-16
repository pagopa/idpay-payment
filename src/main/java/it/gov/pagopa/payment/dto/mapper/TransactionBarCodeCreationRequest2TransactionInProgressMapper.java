package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;

import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionBarCodeCreationRequest2TransactionInProgressMapper {
    private final Clock clock;

    public TransactionBarCodeCreationRequest2TransactionInProgressMapper(Clock clock) {
        this.clock = clock;
    }

    public TransactionInProgress apply(
            TransactionBarCodeCreationRequest transactionBarCodeCreationRequest,
            String channel,
            String userId,
            String initiativeName,
            Map<String, String> additionalProperties,
            boolean extendedAuthorization,
            Instant trxEndDate
    ) {
        String id =
                "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());

        Instant now = Instant.now(clock);

        return TransactionInProgress.builder()
                .id(id)
                .correlationId(id)
                .initiativeId(transactionBarCodeCreationRequest.getInitiativeId())
                .initiatives(List.of(transactionBarCodeCreationRequest.getInitiativeId()))
                .initiativeName(initiativeName)
                .trxDate(now)
                .status(SyncTrxStatus.CREATED)
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .channel(channel)
                .userId(userId)
                .updateDate(now)
                .additionalProperties(additionalProperties)
                .extendedAuthorization(extendedAuthorization)
                .trxEndDate(trxEndDate)
                .voucherAmountCents(transactionBarCodeCreationRequest.getVoucherAmountCents())
                .build();
    }
}
