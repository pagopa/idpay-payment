package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TransactionInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress, TransactionResponse> {

  @Override
  public TransactionResponse apply(TransactionInProgress transactionInProgress) {
    return TransactionResponse.builder()
        .acquirerId(transactionInProgress.getAcquirerId())
        .amountCents(transactionInProgress.getAmountCents())
        .amountCurrency(transactionInProgress.getAmountCurrency())
        .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
        .idTrxIssuer(transactionInProgress.getIdTrxIssuer())
        .initiativeId(transactionInProgress.getInitiativeId())
        .mcc(transactionInProgress.getMcc())
        .id(transactionInProgress.getId())
        .merchantId(transactionInProgress.getMerchantId())
        .trxDate(transactionInProgress.getTrxDate())
        .trxCode(transactionInProgress.getTrxCode())
        .status(transactionInProgress.getStatus())
        .merchantFiscalCode(transactionInProgress.getMerchantFiscalCode())
        .vat(transactionInProgress.getVat())
        .build();
  }
}
