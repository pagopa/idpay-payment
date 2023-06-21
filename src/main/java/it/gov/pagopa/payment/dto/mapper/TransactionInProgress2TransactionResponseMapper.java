package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class TransactionInProgress2TransactionResponseMapper
    implements TriFunction<TransactionInProgress, String, String, TransactionResponse> {

  @Value("${app.qrCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes;

  @Override
  public TransactionResponse apply(TransactionInProgress transactionInProgress, String qrcodeImgBaseUrl, String qrcodeTxtBaseUrl) {
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
            .qrcodePngUrl(generateTrxCodeImgUrl(qrcodeImgBaseUrl, transactionInProgress.getTrxCode()))
            .qrcodeTxtUrl(generateTrxCodeTxtUrl(qrcodeTxtBaseUrl, transactionInProgress.getTrxCode()))
            .build();
  }

  public static String generateTrxCodeImgUrl(String imgBaseUrl, String trxCode){
    if(imgBaseUrl != null) {
      try {
        return UriComponentsBuilder.fromUriString(imgBaseUrl).queryParam("trxcode", trxCode).build().toString();
      } catch (Exception e) {
        log.error("Something went wrong with generated url for trxCode image", e);
      }
    }
    return null;
  }

  public static String generateTrxCodeTxtUrl(String txtBaseUrl, String trxCode){
    if(txtBaseUrl != null) {
      try {
        return txtBaseUrl.concat("/%s".formatted(trxCode));
      } catch (Exception e) {
        log.error("Something went wrong with generated url for trxCode txt", e);
      }
    }
    return null;
  }
}
