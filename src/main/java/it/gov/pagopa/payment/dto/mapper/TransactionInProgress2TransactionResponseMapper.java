package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TransactionInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress, TransactionResponse> {

  @Override
  public TransactionResponse apply(TransactionInProgress transactionInProgress) {
    TransactionResponse transactionResponse= TransactionResponse.builder()
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
    TransactionInProgress2TransactionResponseMapper.setResidual(transactionResponse, transactionInProgress.getReward());
    return transactionResponse;
  }

  public static void setResidual(TransactionResponse transactionResponse, Long transactionInProgressReward){
    if(transactionResponse.getAmountCents() != null && transactionInProgressReward != null) {
      transactionResponse.setResidualAmountCents(transactionResponse.getAmountCents() - transactionInProgressReward);
      transactionResponse.setSplitPayment(transactionResponse.getResidualAmountCents()> 0L);
    }
  }
}
