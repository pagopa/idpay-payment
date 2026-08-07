package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TransactionMapper {

    private final int authorizationExpirationMinutes;
    private final int commonAuthorizationExpirationMinutes;
    private final String imgBaseUrl;
    private final String txtBaseUrl;

    public TransactionMapper(@Value("${app.bar-code.expirations.authorization-minutes}") int authorizationExpirationMinutes,
                             @Value("${app.common.expirations.authorizationMinutes}") int commonAuthorizationExpirationMinutes,
                             @Value("${app.qrCode.trxCode.baseUrl.img}") String imgBaseUrl,
                             @Value("${app.qrCode.trxCode.baseUrl.txt}") String txtBaseUrl) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.commonAuthorizationExpirationMinutes = commonAuthorizationExpirationMinutes;
        this.imgBaseUrl = imgBaseUrl;
        this.txtBaseUrl = txtBaseUrl;
    }

    public SyncTrxStatusDTO transactionToSyncTrxStatus(Transaction transaction){

        Pair<Boolean, Long> splitAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

        SyncTrxStatusDTO response = SyncTrxStatusDTO.builder()
                .id(transaction.getId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxCode(transaction.getTrxCode())
                .trxDate(transaction.getTrxDate())
                .authDate(transaction.getTrxChargeDate() == null ? null : transaction.getTrxChargeDate())
                .operationType(transaction.getOperationTypeTranscoded())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .merchantId(transaction.getMerchantId())
                .initiativeId(transaction.getInitiativeId())
                .rewardCents(transaction.getRewardCents())
                .rejectionReasons(transaction.getRejectionReasons())
                .status(transaction.getStatus())
                .splitPayment(splitAndResidualAmountCents.getKey())
                .residualAmountCents(splitAndResidualAmountCents.getValue())
                .build();

        if(evaluateTransactionStatusAndChannel(transaction)){
            response.setQrcodePngUrl(generateTrxCodeImgUrl(transaction.getTrxCode()));
            response.setQrcodeTxtUrl(generateTrxCodeTxtUrl(transaction.getTrxCode()));
        }

        return response;
    }

    private boolean evaluateTransactionStatusAndChannel(Transaction transaction){
        return (SyncTrxStatus.CREATED.equals(transaction.getStatus()) && !RewardConstants.TRX_CHANNEL_BARCODE.equals(transaction.getChannel()))
                || (!SyncTrxStatus.CREATED.equals(transaction.getStatus()) && RewardConstants.TRX_CHANNEL_QRCODE.equals(transaction.getChannel()));
    }

    public TransactionBarCodeResponse transactionBarCodeToTransactionResponse(Transaction transactionInProgress) {

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

    public Transaction transactionBarCodeCreationRequestToTransaction(
            TransactionBarCodeCreationRequest transactionBarCodeCreationRequest,
            String channel,
            String userId,
            String initiativeName,
            Map<String, String> additionalProperties,
            boolean extendedAuthorization,
            OffsetDateTime trxEndDate
    ) {
        String id =
                "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Rome"));

        return Transaction.builder()
                .id(id)
                .correlationId(id)
                .initiativeId(transactionBarCodeCreationRequest.getInitiativeId())
                .initiatives(List.of(transactionBarCodeCreationRequest.getInitiativeId()))
                .initiativeName(initiativeName)
                .trxDate(now)
                .status(SyncTrxStatus.CREATED)
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .channel(channel)
                .userId(userId)
                .updateDate(now.toLocalDateTime())
                .additionalProperties(additionalProperties)
                .extendedAuthorization(extendedAuthorization)
                .trxEndDate(trxEndDate)
                .voucherAmountCents(transactionBarCodeCreationRequest.getVoucherAmountCents())
                .transactionRevision(0L)
                .build();
    }


    public TransactionBarCodeResponse transactionToTransactionBarCodeResponse(Transaction transaction) {

        Long authorizationExpiration = Boolean.TRUE.equals(transaction.getExtendedAuthorization()) ?
                CommonUtilities.secondsBetween(transaction.getTrxDate(), transaction.getTrxEndDate())
                : CommonUtilities.minutesToSeconds(authorizationExpirationMinutes);

        return TransactionBarCodeResponse.builder()
                .id(transaction.getId())
                .trxCode(transaction.getTrxCode())
                .initiativeId(transaction.getInitiativeId())
                .initiativeName(transaction.getInitiativeName())
                .trxDate(transaction.getTrxDate())
                .trxExpirationSeconds(authorizationExpiration)
                .status(transaction.getStatus())
                .residualBudgetCents(transaction.getAmountCents())
                .trxEndDate(transaction.getTrxEndDate())
                .voucherAmountCents(transaction.getVoucherAmountCents())
                .build();
    }


    public Transaction transactionCreationRequestToTransaction(
            TransactionCreationRequest transactionCreationRequest,
            String channel,
            String merchantId,
            String acquirerId,
            MerchantDetailDTO merchantDetail,
            String idTrxIssuer) {
        String id =
                "%s_%d".formatted(UUID.randomUUID().toString(), System.currentTimeMillis());

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Rome"));

        return Transaction.builder()
                .id(id)
                .correlationId(id)
                .amountCents(transactionCreationRequest.getAmountCents())
                .effectiveAmountCents(transactionCreationRequest.getAmountCents())
                .amountCurrency(PaymentConstants.CURRENCY_EUR)
                .merchantFiscalCode(merchantDetail.getFiscalCode())
                .idTrxIssuer(idTrxIssuer)
                .initiativeId(transactionCreationRequest.getInitiativeId())
                .initiatives(List.of(transactionCreationRequest.getInitiativeId()))
                .initiativeName(merchantDetail.getInitiativeName())
                .businessName(merchantDetail.getBusinessName())
                .mcc(transactionCreationRequest.getMcc())
                .vat(merchantDetail.getVatNumber())
                .trxDate(now)
                .status(SyncTrxStatus.CREATED)
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .channel(channel)
                .merchantId(merchantId)
                .acquirerId(acquirerId)
                .idTrxAcquirer(transactionCreationRequest.getIdTrxAcquirer())
                .updateDate(now.toLocalDateTime())
                .counterVersion(0L)
                .transactionRevision(0L)
                .additionalProperties(transactionCreationRequest.getAdditionalProperties())
                .build();
    }


    public TransactionResponse transactionToTransactionResponse(Transaction transaction) {
        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

        return TransactionResponse.builder()
                .acquirerId(transaction.getAcquirerId())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .idTrxAcquirer(transaction.getIdTrxAcquirer())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .initiativeId(transaction.getInitiativeId())
                .mcc(transaction.getMcc())
                .id(transaction.getId())
                .merchantId(transaction.getMerchantId())
                .trxDate(transaction.getTrxDate())
                .trxCode(transaction.getTrxCode())
                .status(transaction.getStatus())
                .merchantFiscalCode(transaction.getMerchantFiscalCode())
                .vat(transaction.getVat())
                .splitPayment(splitPaymentAndResidualAmountCents.getKey())
                .residualAmountCents(splitPaymentAndResidualAmountCents.getValue())
                .trxExpirationSeconds(CommonUtilities.minutesToSeconds(commonAuthorizationExpirationMinutes))
                .qrcodePngUrl(generateTrxCodeImgUrl(transaction.getTrxCode()))
                .qrcodeTxtUrl(generateTrxCodeTxtUrl(transaction.getTrxCode()))
                .additionalProperties(transaction.getAdditionalProperties())
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
