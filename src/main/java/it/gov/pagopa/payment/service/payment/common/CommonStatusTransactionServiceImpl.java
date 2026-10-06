package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommonStatusTransactionServiceImpl {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    private static final String TRANSACTION_NOT_FOUND_MESSAGE =
            "Cannot find transaction with transactionId [%s]";

    public CommonStatusTransactionServiceImpl(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(TRANSACTION_NOT_FOUND_MESSAGE.formatted(transactionId)));

        if(!transaction.getMerchantId().equals(merchantId)){
            log.info("Merchant " + merchantId + " not authorized to retrieve transaction " + transactionId);
            throw new TransactionNotFoundOrExpiredException(TRANSACTION_NOT_FOUND_MESSAGE.formatted(transactionId));
        }

        return transactionMapper.transactionToSyncTrxStatus(transaction);
    }
}