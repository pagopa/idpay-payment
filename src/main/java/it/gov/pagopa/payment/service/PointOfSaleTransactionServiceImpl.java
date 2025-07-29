package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class PointOfSaleTransactionServiceImpl implements PointOfSaleTransactionService {

    private final int authorizationExpirationMinutes;

    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    public PointOfSaleTransactionServiceImpl(
            @Value("${app.common.expirations.authorizationMinutes}") int authorizationExpirationMinutes,

            DecryptRestConnector decryptRestConnector,
            EncryptRestConnector encryptRestConnector,
            TransactionInProgressRepository transactionInProgressRepository,
            TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.decryptRestConnector = decryptRestConnector;
        this.encryptRestConnector = encryptRestConnector;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    }

    @Override
    public PointOfSaleTransactionsListDTO getPointOfSaleTransactions(String merchantId, String initiativeId, String pointOfSaleId, String fiscalCode, String status, Pageable pageable) {
        String userId = null;
        if (StringUtils.isNotBlank(fiscalCode)) {
            userId = encryptCF(fiscalCode);
        }
        Criteria criteria = transactionInProgressRepository.getCriteria(merchantId, pointOfSaleId, initiativeId, userId, status);
        List<TransactionInProgress> transactionInProgressList = transactionInProgressRepository.findByFilter(criteria, pageable);
        List<PointOfSaleTransactionDTO> pointOfSaleTransactions = new ArrayList<>();
        transactionInProgressList.forEach(
                transaction ->
                        pointOfSaleTransactions.add(
                                populatePointOfSaleTransactionDTO(
                                        transaction,
                                        StringUtils.isNotBlank(fiscalCode) ? fiscalCode : decryptCF(transaction.getUserId())
                                )
                        )
        );

        long count = transactionInProgressRepository.getCount(criteria);
        final Page<TransactionInProgress> result = PageableExecutionUtils.getPage(transactionInProgressList,
                CommonUtilities.getPageable(pageable), () -> count);
        return new PointOfSaleTransactionsListDTO(pointOfSaleTransactions, result.getNumber(), result.getSize(),
                (int) result.getTotalElements(), result.getTotalPages());
    }

    private PointOfSaleTransactionDTO populatePointOfSaleTransactionDTO(TransactionInProgress transaction, String fiscalCode) {
        String trxCodeImgUrl = null;
        String trxCodeTxtUrl = null;

        if (null == transaction.getChannel() || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(transaction.getChannel())) {
            trxCodeImgUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(transaction.getTrxCode());
            trxCodeTxtUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(transaction.getTrxCode());
        }
        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

        return new PointOfSaleTransactionDTO(transaction.getTrxCode(),
                transaction.getCorrelationId(),
                fiscalCode,
                transaction.getAmountCents(),
                transaction.getRewardCents() != null ? transaction.getRewardCents() : Long.valueOf(0),
                transaction.getTrxDate().toLocalDateTime(),
                CommonUtilities.minutesToSeconds(authorizationExpirationMinutes),
                transaction.getUpdateDate(),
                transaction.getStatus(),
                splitPaymentAndResidualAmountCents.getKey(),
                splitPaymentAndResidualAmountCents.getValue(),
                transaction.getChannel(),
                trxCodeImgUrl,
                trxCodeTxtUrl
        );
    }

    private String decryptCF(String userId) {
        return wrapPDVCall(() -> decryptRestConnector.getPiiByToken(userId).getPii(),
                "An error occurred during decryption");
    }

    private String encryptCF(String fiscalCode) {
        return wrapPDVCall(() -> encryptRestConnector.upsertToken(new CFDTO(fiscalCode)).getToken(),
                "An error occurred during encryption");
    }

    private <T> T wrapPDVCall(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            throw new PDVInvocationException(errorMessage, true, e);
        }
    }
}
