package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TransactionInProgress2TransactionCreatedMapper
    implements Function<TransactionInProgress, TransactionCreated> {

  @Override
  public TransactionCreated apply(TransactionInProgress transactionInProgress) {
    return TransactionCreated.builder()
        .acquirerId(transactionInProgress.getAcquirerId())
        .acquirerCode(transactionInProgress.getAcquirerCode())
        .amount(transactionInProgress.getAmount())
        .amountCurrency(transactionInProgress.getAmountCurrency())
        .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
        .idTrxIssuer(transactionInProgress.getIdTrxIssuer())
        .initiativeId(transactionInProgress.getInitiativeId())
        .mcc(transactionInProgress.getMcc())
        .id(transactionInProgress.getId())
        .merchantId(transactionInProgress.getMerchantId())
        .senderCode(transactionInProgress.getSenderCode())
        .trxDate(transactionInProgress.getTrxDate())
        .trxCode(transactionInProgress.getTrxCode())
        .build();
  }
}
