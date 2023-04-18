package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.BiFunction;

@Service
public class TransactionCreationRequest2TransactionInProgressMapper
    implements BiFunction<TransactionCreationRequest, String, TransactionInProgress> {

  @Override
  public TransactionInProgress apply(TransactionCreationRequest transactionCreationRequest, String channel) {
    String id = "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());
    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .amountCents(transactionCreationRequest.getAmountCents())
        .effectiveAmount(Utils.centsToEuro(transactionCreationRequest.getAmountCents()))
        .amountCurrency(PaymentConstants.CURRENCY_EUR)
        .merchantFiscalCode(transactionCreationRequest.getMerchantFiscalCode())
        .callbackUrl(transactionCreationRequest.getCallbackUrl())
        .idTrxIssuer(transactionCreationRequest.getIdTrxIssuer())
        .initiativeId(transactionCreationRequest.getInitiativeId())
        .mcc(transactionCreationRequest.getMcc())
        .vat(transactionCreationRequest.getVat())
        .trxDate(transactionCreationRequest.getTrxDate())
        .trxChargeDate(transactionCreationRequest.getTrxDate())
        .status(SyncTrxStatus.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .channel(channel)
        .build();
  }
}
