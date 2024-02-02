package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionCreationRequest2TransactionInProgressMapper {

  public TransactionInProgress apply(
          TransactionCreationRequest transactionCreationRequest,
          String channel,
          String merchantId,
          String acquirerId,
          MerchantDetailDTO merchantDetail,
          String idTrxIssuer) {
    String id =
        "%s_%d".formatted(UUID.randomUUID().toString(), System.currentTimeMillis());

    OffsetDateTime now = OffsetDateTime.now();

    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .amountCents(transactionCreationRequest.getAmountCents())
        .effectiveAmount(CommonUtilities.centsToEuro(transactionCreationRequest.getAmountCents()))
        .amountCurrency(PaymentConstants.CURRENCY_EUR)
        .merchantFiscalCode(merchantDetail.getFiscalCode())
        .idTrxIssuer(idTrxIssuer)
        .initiativeId(transactionCreationRequest.getInitiativeId())
        .initiatives(List.of(transactionCreationRequest.getInitiativeId()))
        .initiativeName(merchantDetail.getInitiativeName())
        .businessName(merchantDetail.getBusinessName())
        .mcc(transactionCreationRequest.getMcc())
        .vat(merchantDetail.getVatNumber())
        .trxDate(now)
        .status(SyncTrxStatus.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .channel(channel)
        .merchantId(merchantId)
        .acquirerId(acquirerId)
        .idTrxAcquirer(transactionCreationRequest.getIdTrxAcquirer())
        .updateDate(now.toLocalDateTime())
        .build();
  }
}
