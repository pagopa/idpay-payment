package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Service
@Slf4j
public class TransactionBarCodeInProgress2TransactionEnrichedResponseMapper
    implements Function<TransactionInProgress, TransactionBarCodeEnrichedResponse> {

  private final int authorizationExpirationMinutes;
  private final int extendedAuthorizationExpirationMinutes;

  public TransactionBarCodeInProgress2TransactionEnrichedResponseMapper(@Value("${app.barCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes,
                                                                        @Value("${app.barCode.expirations.extendedAuthorizationMinutes}") int extendedAuthorizationExpirationMinutes) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    this.extendedAuthorizationExpirationMinutes = extendedAuthorizationExpirationMinutes;
  }
  @Override
  public TransactionBarCodeEnrichedResponse apply(TransactionInProgress transactionInProgress) {

    int authorizationExpiration = Boolean.TRUE.equals(transactionInProgress.getExtendedAuthorization()) ? extendedAuthorizationExpirationMinutes : authorizationExpirationMinutes;
    OffsetDateTime endDate = calculateTrxEndDate(transactionInProgress);

    return TransactionBarCodeEnrichedResponse.builder()
            .id(transactionInProgress.getId())
            .trxCode(transactionInProgress.getTrxCode())
            .initiativeId(transactionInProgress.getInitiativeId())
            .initiativeName(transactionInProgress.getInitiativeName())
            .trxDate(transactionInProgress.getTrxDate())
            .trxExpirationSeconds(CommonUtilities.minutesToSeconds(authorizationExpiration))
            .status(transactionInProgress.getStatus())
            .residualBudgetCents(transactionInProgress.getAmountCents())
            .trxEndDate(endDate)
            .build();
  }

  private OffsetDateTime calculateTrxEndDate(TransactionInProgress transactionInProgress) {
    if (Boolean.TRUE.equals(transactionInProgress.getExtendedAuthorization())){
      OffsetDateTime endDate = transactionInProgress.getTrxDate().plusMinutes(authorizationExpirationMinutes);
      return endDate.truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);
    }

    return transactionInProgress.getTrxDate().plusMinutes(authorizationExpirationMinutes);
  }
}
