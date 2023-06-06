package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class MerchantTransactionServiceImpl implements MerchantTransactionService{

    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    @Value("${app.qrCode.trxInProgressLifetimeMinutes}") int trxInProgressLifetimeMinutes;

    public MerchantTransactionServiceImpl(DecryptRestConnector decryptRestConnector, EncryptRestConnector encryptRestConnector, TransactionInProgressRepository transactionInProgressRepository) {
        this.decryptRestConnector = decryptRestConnector;
        this.encryptRestConnector = encryptRestConnector;
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public MerchantTransactionsListDTO getMerchantTransactions(String merchantId, String initiativeId, String fiscalCode, String status, Pageable pageable){
        String userId = null;
        if(StringUtils.isNotBlank(fiscalCode)){
            userId = encryptCF(fiscalCode);
        }
        Criteria criteria = transactionInProgressRepository.getCriteria(merchantId, initiativeId, userId, status);
        List<TransactionInProgress> transactionInProgressList = transactionInProgressRepository.findByFilter(criteria, pageable);
        List<MerchantTransactionDTO> merchantTransactions = new ArrayList<>();
        if (!transactionInProgressList.isEmpty()) {
            transactionInProgressList.forEach(
                    transaction -> merchantTransactions.add(
                                    new MerchantTransactionDTO(
                                            transaction.getTrxCode(),
                                            transaction.getCorrelationId(),
                                            transaction.getUserId() != null ? decryptCF(transaction.getUserId()) : null,
                                            transaction.getEffectiveAmount(),
                                            transaction.getTrxDate().toLocalDateTime(),
                                            trxInProgressLifetimeMinutes,
                                            transaction.getUpdateDate(),
                                            transaction.getStatus().toString()
                                    )));
        }
        long count = transactionInProgressRepository.getCount(criteria);
        final Page<TransactionInProgress> result = PageableExecutionUtils.getPage(transactionInProgressList,
                CommonUtilities.getPageable(pageable), () -> count);
        return new MerchantTransactionsListDTO(merchantTransactions, result.getNumber(), result.getSize(),
                (int) result.getTotalElements(), result.getTotalPages());
    }

    private String decryptCF(String userId) {
        String fiscalCode;
        try {
            DecryptCfDTO decryptedCfDTO = decryptRestConnector.getPiiByToken(userId);
            fiscalCode = decryptedCfDTO.getPii();
        } catch (Exception e) {
            throw new ClientExceptionWithBody(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL SERVER ERROR",
                    "Error during decryption, userId: [%s]".formatted(userId));
        }
        return fiscalCode;
    }

    private String encryptCF(String fiscalCode) {
        String userId;
        try {
            EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
            userId = encryptedCfDTO.getToken();
        } catch (Exception e) {
            throw new ClientExceptionWithBody(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL SERVER ERROR",
                    "Error during encryption");
        }
        return userId;
    }
}
