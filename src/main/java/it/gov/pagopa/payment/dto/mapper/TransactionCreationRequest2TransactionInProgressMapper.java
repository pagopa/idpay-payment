package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.Status;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TransactionCreationRequest2TransactionInProgressMapper
    implements Function<TransactionCreationRequest, TransactionInProgress> {

  @Override
  public TransactionInProgress apply(TransactionCreationRequest transactionCreationRequest) {
    return TransactionInProgress.builder()
        .acquirerId(transactionCreationRequest.getAcquirerId())
        .acquirerCode(transactionCreationRequest.getAcquirerCode())
        .amount(transactionCreationRequest.getAmount())
        .effectiveAmount(transactionCreationRequest.getAmount())
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
        .status(Status.CREATED)
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .build();
  }
}
