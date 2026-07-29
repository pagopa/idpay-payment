package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.service.PDVService;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Component
public class PointOfSaleTransactionMapper {

    private final int authorizationExpirationMinutes;
    private final PDVService pdvService;
    private final String imgBaseUrl;
    private final String txtBaseUrl;

    public PointOfSaleTransactionMapper(
            PDVService pdvService,
            @Value("${app.common.expirations.authorizationMinutes}") int authorizationExpirationMinutes,
            @Value("${app.qrCode.trxCode.baseUrl.img}") String imgBaseUrl,
            @Value("${app.qrCode.trxCode.baseUrl.txt}") String txtBaseUrl) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.pdvService = pdvService;
        this.imgBaseUrl = imgBaseUrl;
        this.txtBaseUrl = txtBaseUrl;
    }

    public PointOfSaleTransactionDTO toPointOfSaleTransactionDTO(Transaction trx, String fiscalCodeInput) {
        Long totalAmount = trx.getAmountCents();

        Long rewardAmount = 0L;

        if (trx.getRewards() != null && trx.getRewards().get(trx.getInitiativeId()) != null) {
            rewardAmount = Math.abs(trx.getRewards().get(trx.getInitiativeId()).getAccruedRewardCents());
        }

        Long authorizedAmount = totalAmount - rewardAmount;

        InvoiceData invoiceFile = null;

        if ((SyncTrxStatus.INVOICED.equals(trx.getStatus())
                || SyncTrxStatus.REWARDED.equals(trx.getStatus()))
                && trx.getInvoiceData() != null) {
            invoiceFile = InvoiceData.builder()
                    .filename(trx.getInvoiceData().getFilename())
                    .docNumber(trx.getInvoiceData().getDocNumber())
                    .build();
        } else if (SyncTrxStatus.REFUNDED.equals(trx.getStatus())
                && trx.getCreditNoteData() != null) {
            invoiceFile = InvoiceData.builder()
                    .filename(trx.getCreditNoteData().getFilename())
                    .docNumber(trx.getCreditNoteData().getDocNumber())
                    .build();
        }

        String fiscalCode = resolveFiscalCode(fiscalCodeInput, trx.getUserId());

        String[] trxCodeUrls = resolveTrxCodeUrls(trx);

        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(trx.getAmountCents(), trx.getRewardCents());


        return PointOfSaleTransactionDTO.builder()
                //processed
                .trxId(trx.getId())
                .trxCode(trx.getTrxCode())
                .effectiveAmountCents(trx.getAmountCents())
                .rewardAmountCents(rewardAmount)
                .authorizedAmountCents(authorizedAmount)
                .trxDate(trx.getTrxDate().atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime())
                .trxChargeDate(trx.getTrxChargeDate().atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime())
                .status(String.valueOf(trx.getStatus()))
                .rewardBatchTrxStatus(trx.getRewardBatchStatusTrx() != null ? trx.getRewardBatchStatusTrx() : null)
                .channel(trx.getChannel())
                .fiscalCode(fiscalCode)
                .additionalProperties(trx.getAdditionalProperties())
                .invoiceData(invoiceFile)
                //
                .trxExpirationSeconds(CommonUtilities.minutesToSeconds(authorizationExpirationMinutes))
                .updateDate(trx.getUpdateDate())
                .splitPayment(splitPaymentAndResidualAmountCents.getKey())
                .residualAmountCents(splitPaymentAndResidualAmountCents.getValue())
                .qrcodePngUrl(trxCodeUrls[0])
                .qrcodeTxtUrl(trxCodeUrls[1])
                .build();
    }

    private String resolveFiscalCode(String fiscalCodeInput, String userId) {
        if (StringUtils.isNotBlank(fiscalCodeInput)) {
            return fiscalCodeInput;
        }
        return userId != null ? pdvService.decryptCF(userId) : null;
    }

    private String[] resolveTrxCodeUrls(Transaction trx) {
        if (trx.getChannel() == null || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(trx.getChannel())) {
            return new String[]{
                    generateTrxCodeImgUrl(trx.getTrxCode()),
                    generateTrxCodeTxtUrl(trx.getTrxCode())
            };
        }
        return new String[]{null, null};
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
