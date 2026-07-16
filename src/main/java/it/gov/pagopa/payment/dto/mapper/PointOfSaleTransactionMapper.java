package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.entity.Transaction;
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

    public PointOfSaleTransactionDTO toPointOfSaleTransactionDTO(Transaction trx, String fiscalCodeInput) {
        String fiscalCode = resolveFiscalCode(fiscalCodeInput, trx.getUserId());
        String[] trxCodeUrls = resolveTrxCodeUrls(trx);

        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(trx.getAmountCents(), trx.getRewardCents());

        return new PointOfSaleTransactionDTO(
                trx.getTrxCode(),
                trx.getCorrelationId(),
                fiscalCode,
                trx.getAmountCents(),
                trx.getRewardCents() != null ? trx.getRewardCents() : Long.valueOf(0),
                trx.getTrxDate().toLocalDateTime(),
                trx.getTrxChargeDate().toLocalDateTime(),
                CommonUtilities.minutesToSeconds(authorizationExpirationMinutes),
                trx.getUpdateDate(),
                trx.getStatus(),
                splitPaymentAndResidualAmountCents.getKey(),
                splitPaymentAndResidualAmountCents.getValue(),
                trx.getChannel(),
                trxCodeUrls[0],
                trxCodeUrls[1],
                trx.getAdditionalProperties()
        );
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
                    transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trx.getTrxCode()),
                    transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trx.getTrxCode())
            };
        }
        return new String[]{null, null};
    }
}
