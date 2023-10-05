package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class TransactionBarCodeInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress, TransactionBarCodeResponse> {

  private final int authorizationExpirationMinutes;

  public TransactionBarCodeInProgress2TransactionResponseMapper(@Value("${app.barCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;

  }
  @Override
  public TransactionBarCodeResponse apply(TransactionInProgress transactionInProgress) {

    return TransactionBarCodeResponse.builder()
            .id(transactionInProgress.getId())
            .trxCode(transactionInProgress.getTrxCode())
            .initiativeId(transactionInProgress.getInitiativeId())
            .trxDate(transactionInProgress.getTrxDate())
            .trxExpirationMinutes(authorizationExpirationMinutes)
            .status(transactionInProgress.getStatus())
            .build();
  }
}
