package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TransactionBarCodeCreationRequest2TransactionInProgressMapper {

  public TransactionInProgress apply(
          TransactionBarCodeCreationRequest transactionBarCodeCreationRequest,
          String channel,
          String userId
          ) {
    String id =
        "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());

    OffsetDateTime now = OffsetDateTime.now();

    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .initiativeId(transactionBarCodeCreationRequest.getInitiativeId())
        .trxDate(now)
        .status(SyncTrxStatus.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .channel(channel)
        .userId(userId)
        .updateDate(now.toLocalDateTime())
        .build();
  }
}
