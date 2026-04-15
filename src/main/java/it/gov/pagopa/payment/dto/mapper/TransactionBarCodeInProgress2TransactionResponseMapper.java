package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Service
@Slf4j
public class TransactionBarCodeInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress, TransactionBarCodeResponse> {

  private final int authorizationExpirationMinutes;
  private final int extendedAuthorizationExpirationMinutes;

  public TransactionBarCodeInProgress2TransactionResponseMapper(@Value("${app.bar-code.expirations.authorization-minutes}") int authorizationExpirationMinutes,
                                                                @Value("${app.bar-code.expirations.extended-authorization-minutes}") int extendedAuthorizationExpirationMinutes) {
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    this.extendedAuthorizationExpirationMinutes = extendedAuthorizationExpirationMinutes;
  }
  @Override
  public TransactionBarCodeResponse apply(TransactionInProgress transactionInProgress) {

    Long authorizationExpiration = Boolean.TRUE.equals(transactionInProgress.getExtendedAuthorization()) ?
            CommonUtilities.secondsBetween(transactionInProgress.getTrxDate(), transactionInProgress.getTrxEndDate())
            : CommonUtilities.minutesToSeconds(authorizationExpirationMinutes);

    return TransactionBarCodeResponse.builder()
            .id(transactionInProgress.getId())
            .trxCode(transactionInProgress.getTrxCode())
            .initiativeId(transactionInProgress.getInitiativeId())
            .initiativeName(transactionInProgress.getInitiativeName())
            .trxDate(transactionInProgress.getTrxDate())
            .trxExpirationSeconds(authorizationExpiration)
            .status(transactionInProgress.getStatus())
            .residualBudgetCents(transactionInProgress.getAmountCents())
            .trxEndDate(transactionInProgress.getTrxEndDate())
            .voucherAmountCents(transactionInProgress.getVoucherAmountCents())
            .build();
  }

  public Instant calculateTrxEndDate(TransactionInProgress transactionInProgress) {
    if (Boolean.TRUE.equals(transactionInProgress.getExtendedAuthorization())){
      return calculateExtendedEndDate(transactionInProgress, extendedAuthorizationExpirationMinutes);
    }

    return transactionInProgress.getTrxDate().plus(authorizationExpirationMinutes,ChronoUnit.MINUTES);
  }

  public static Instant calculateExtendedEndDate(TransactionInProgress trx, int authExpirationMinutes) {

    Instant trxExpiry =
            trx.getTrxDate().plus(authExpirationMinutes, ChronoUnit.MINUTES);

    Instant initiativeEnd = trx.getInitiativeEndDate();

    Instant effectiveEnd =
            initiativeEnd != null &&
                    initiativeEnd.minus(authExpirationMinutes, ChronoUnit.MINUTES)
                            .isBefore(trx.getTrxDate())
                    ? initiativeEnd
                    : trxExpiry;

    return effectiveEnd
            .atZone(CommonConstants.ZONEID)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(CommonConstants.ZONEID)
            .toInstant();
  }

}
