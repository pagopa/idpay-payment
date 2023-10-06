package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class TransactionInProgress2BaseTransactionResponseMapper
    implements Function<TransactionInProgress, BaseTransactionResponseDTO> {

  private final int authorizationExpirationMinutes;

  public TransactionInProgress2BaseTransactionResponseMapper(@Value("${app.idpayCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
  }

  @Override
  public BaseTransactionResponseDTO apply(TransactionInProgress transactionInProgress) {
    Long residualAmountCents = null;
    Boolean splitPayment = null;
    if (transactionInProgress.getAmountCents() != null && transactionInProgress.getReward() != null) {
      residualAmountCents = transactionInProgress.getAmountCents() - transactionInProgress.getReward();
      splitPayment = residualAmountCents > 0L;
    }
    return BaseTransactionResponseDTO.builder()
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
            .splitPayment(splitPayment)
            .residualAmountCents(residualAmountCents)
            .trxExpirationMinutes(authorizationExpirationMinutes)
            .build();
  }

}
