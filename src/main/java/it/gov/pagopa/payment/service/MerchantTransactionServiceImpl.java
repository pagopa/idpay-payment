package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import java.util.ArrayList;
import java.util.List;

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

@Service
public class MerchantTransactionServiceImpl implements MerchantTransactionService {

    private final int authorizationExpirationMinutes;

    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    public MerchantTransactionServiceImpl(
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
    public MerchantTransactionsListDTO getMerchantTransactions(String merchantId, String initiativeId, String fiscalCode, String status, Pageable pageable) {
        String userId = null;
        if (StringUtils.isNotBlank(fiscalCode)) {
            userId = encryptCF(fiscalCode);
        }
        Criteria criteria = transactionInProgressRepository.getCriteria(merchantId, null, initiativeId, userId, status, null, null);
        List<TransactionInProgress> transactionInProgressList = transactionInProgressRepository.findByFilter(criteria, pageable);
        List<MerchantTransactionDTO> merchantTransactions = new ArrayList<>();
        if (!transactionInProgressList.isEmpty()) {
            transactionInProgressList.forEach(
                    transaction ->
                            merchantTransactions.add(populateMerchantTransactionDTO(transaction)));
        }
        long count = transactionInProgressRepository.getCount(criteria);
        final Page<TransactionInProgress> result = PageableExecutionUtils.getPage(transactionInProgressList,
                CommonUtilities.getPageable(pageable), () -> count);
        return new MerchantTransactionsListDTO(merchantTransactions, result.getNumber(), result.getSize(),
                (int) result.getTotalElements(), result.getTotalPages());
    }
private MerchantTransactionDTO populateMerchantTransactionDTO(TransactionInProgress transaction){
        String trxCodeImgUrl = null;
        String trxCodeTxtUrl = null;

        if(null == transaction.getChannel() || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(transaction.getChannel())) {
            trxCodeImgUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(transaction.getTrxCode());
            trxCodeTxtUrl = transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(transaction.getTrxCode());
        }
    Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

    return new MerchantTransactionDTO(transaction.getTrxCode(),
                transaction.getCorrelationId(),
                transaction.getUserId() != null ? decryptCF(transaction.getUserId()) : null,
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
                trxCodeTxtUrl,
                transaction.getAdditionalProperties()
                );
        }

    private String decryptCF(String userId) {
        String fiscalCode;
        try {
            DecryptCfDTO decryptedCfDTO = decryptRestConnector.getPiiByToken(userId);
            fiscalCode = decryptedCfDTO.getPii();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during decryption",true,e);
        }
        return fiscalCode;
    }

    private String encryptCF(String fiscalCode) {
        String userId;
        try {
            EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
            userId = encryptedCfDTO.getToken();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during encryption",true,e);
        }
        return userId;
    }

}
