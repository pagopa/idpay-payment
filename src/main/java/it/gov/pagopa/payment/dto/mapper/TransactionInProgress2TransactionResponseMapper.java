package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.function.Function;

@Service
@Slf4j
public class TransactionInProgress2TransactionResponseMapper
    implements Function<TransactionInProgress,TransactionResponse> {

  private final int commonAuthorizationExpirationMinutes;
  private final String imgBaseUrl;
  private final String txtBaseUrl;

  public TransactionInProgress2TransactionResponseMapper(
                                                         @Value("${app.common.expirations.authorizationMinutes}") int commonAuthorizationExpirationMinutes,
                                                         @Value("${app.qrCode.trxCode.baseUrl.img}") String imgBaseUrl,
                                                         @Value("${app.qrCode.trxCode.baseUrl.txt}") String txtBaseUrl) {
    this.commonAuthorizationExpirationMinutes = commonAuthorizationExpirationMinutes;
    this.imgBaseUrl = imgBaseUrl;
    this.txtBaseUrl = txtBaseUrl;
  }

  @Override
  public TransactionResponse apply(TransactionInProgress transactionInProgress) {
    Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transactionInProgress.getAmountCents(), transactionInProgress.getRewardCents());

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
            .splitPayment(splitPaymentAndResidualAmountCents.getKey())
            .residualAmountCents(splitPaymentAndResidualAmountCents.getValue())
            .trxExpirationSeconds(CommonUtilities.minutesToSeconds(commonAuthorizationExpirationMinutes))
            .qrcodePngUrl(generateTrxCodeImgUrl(transactionInProgress.getTrxCode()))
            .qrcodeTxtUrl(generateTrxCodeTxtUrl(transactionInProgress.getTrxCode()))
            .build();
  }

  public String generateTrxCodeImgUrl(String trxCode){
    try {
      return UriComponentsBuilder.fromUriString(imgBaseUrl).queryParam("trxcode", trxCode).build().toString();
    } catch (Exception e) {
      log.error("Something went wrong with generated url for trxCode image", e);
    }
    return null;
  }

  public String generateTrxCodeTxtUrl(String trxCode){
    try {
      return txtBaseUrl.concat("/%s".formatted(trxCode));
    } catch (Exception e) {
      log.error("Something went wrong with generated url for trxCode txt", e);
    }
    return null;
  }
}
