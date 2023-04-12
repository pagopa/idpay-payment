package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TransactionCreationRequest2TransactionInProgressMapper
    implements Function<TransactionCreationRequest, TransactionInProgress> {

  @Override
  public TransactionInProgress apply(TransactionCreationRequest transactionCreationRequest) {
    String id = "%s_qr-code_%d".formatted(UUID.randomUUID().toString(), System.currentTimeMillis());
    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .acquirerId(transactionCreationRequest.getAcquirerId())
        .acquirerCode(transactionCreationRequest.getAcquirerCode())
        .amountCents(transactionCreationRequest.getAmountCents())
        .effectiveAmount(Utils.centsToEuro(transactionCreationRequest.getAmountCents()))
        .amountCurrency(transactionCreationRequest.getAmountCurrency())
        .merchantFiscalCode(transactionCreationRequest.getMerchantFiscalCode())
        .callbackUrl(transactionCreationRequest.getCallbackUrl())
        .idTrxAcquirer(transactionCreationRequest.getIdTrxAcquirer())
        .idTrxIssuer(transactionCreationRequest.getIdTrxIssuer())
        .initiativeId(transactionCreationRequest.getInitiativeId())
        .mcc(transactionCreationRequest.getMcc())
        .senderCode(transactionCreationRequest.getSenderCode())
        .vat(transactionCreationRequest.getVat())
        .trxDate(transactionCreationRequest.getTrxDate())
        .trxChargeDate(transactionCreationRequest.getTrxDate())
        .status(SyncTrxStatus.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .build();
  }
}
