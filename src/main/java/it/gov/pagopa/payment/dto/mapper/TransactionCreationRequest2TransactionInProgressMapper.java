package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionCreationRequest2TransactionInProgressMapper {

  public TransactionInProgress apply(
          TransactionCreationRequest transactionCreationRequest,
          String channel,
          String merchantId,
          String acquirerId,
          String idTrxAcquirer,
          MerchantDetailDTO merchantDetailDTO) {
    String id =
        "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());
    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .amountCents(transactionCreationRequest.getAmountCents())
        .effectiveAmount(Utils.centsToEuro(transactionCreationRequest.getAmountCents()))
        .amountCurrency(PaymentConstants.CURRENCY_EUR)
        .merchantFiscalCode(merchantDetailDTO.getFiscalCode())
        .idTrxIssuer(transactionCreationRequest.getIdTrxIssuer())
        .initiativeId(transactionCreationRequest.getInitiativeId())
        .initiativeName(merchantDetailDTO.getInitiativeName())
        .businessName(merchantDetailDTO.getBusinessName())
        .mcc(transactionCreationRequest.getMcc())
        .vat(merchantDetailDTO.getVatNumber())
        .trxDate(transactionCreationRequest.getTrxDate())
        .trxChargeDate(transactionCreationRequest.getTrxDate())
        .status(SyncTrxStatus.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .channel(channel)
        .merchantId(merchantId)
        .acquirerId(acquirerId)
        .idTrxAcquirer(idTrxAcquirer)
        .build();
  }
}
