package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.PDVService;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PointOfSaleTransactionMapper {

    private final int authorizationExpirationMinutes;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
    private final PDVService pdvService;

    public PointOfSaleTransactionMapper(
            @Value("${app.common.expirations.authorizationMinutes}") int authorizationExpirationMinutes,
            TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
            PDVService pdvService) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
        this.pdvService = pdvService;
    }

    public PointOfSaleTransactionDTO toPointOfSaleTransactionDTO(TransactionInProgress trx, String fiscalCodeInput) {
        String fiscalCode = StringUtils.isNotBlank(fiscalCodeInput) ? fiscalCodeInput : pdvService.decryptCF(trx.getUserId());

        String trxCodeImgUrl = null;
        String trxCodeTxtUrl = null;
        if (trx.getChannel() == null || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(trx.getChannel())) {
            trxCodeImgUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trx.getTrxCode());
            trxCodeTxtUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trx.getTrxCode());
        }

        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(trx.getAmountCents(), trx.getRewardCents());

        return new PointOfSaleTransactionDTO(
                trx.getTrxCode(),
                trx.getCorrelationId(),
                fiscalCode,
                trx.getAmountCents(),
                trx.getRewardCents() != null ? trx.getRewardCents() : Long.valueOf(0),
                trx.getTrxDate().toLocalDateTime(),
                CommonUtilities.minutesToSeconds(authorizationExpirationMinutes),
                trx.getUpdateDate(),
                trx.getStatus(),
                splitPaymentAndResidualAmountCents.getKey(),
                splitPaymentAndResidualAmountCents.getValue(),
                trx.getChannel(),
                trxCodeImgUrl,
                trxCodeTxtUrl
        );
    }
}
