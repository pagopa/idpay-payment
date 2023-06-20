package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TransactionInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress, TransactionResponse> {

  @Value("${app.qrCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes;

  @Override
  public TransactionResponse apply(TransactionInProgress transactionInProgress) {
    Long residualAmountCents = null;
    Boolean splitPayment = null;
    if (transactionInProgress.getAmountCents() != null && transactionInProgress.getReward() != null) {
      residualAmountCents = transactionInProgress.getAmountCents() - transactionInProgress.getReward();
      splitPayment = residualAmountCents > 0L;
    }
    return TransactionResponse.builder()
            .acquirerId(transactionInProgress.getAcquirerId())
            .amountCents(transactionInProgress.getAmountCents())
            .amountCurrency(transactionInProgress.getAmountCurrency())
            .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
            .initiativeId(transactionInProgress.getInitiativeId())
            .mcc(transactionInProgress.getMcc())
            .id(transactionInProgress.getId())
            .merchantId(transactionInProgress.getMerchantId())
            .trxDate(transactionInProgress.getTrxDate())
            .trxCode(transactionInProgress.getTrxCode())
            .status(transactionInProgress.getStatus())
            .merchantFiscalCode(transactionInProgress.getMerchantFiscalCode())
            .vat(transactionInProgress.getVat())
            .splitPayment(splitPayment)
            .residualAmountCents(residualAmountCents)
            .trxExpirationMinutes(authorizationExpirationMinutes)
            .qrcodePngUrl(transactionInProgress.getQrcodePngUrl())
            .qrcodeTxtUrl(transactionInProgress.getQrcodeTxtUrl())
            .build();
  }
}
