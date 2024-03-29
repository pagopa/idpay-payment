package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommonStatusTransactionServiceImpl {
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper;

    public CommonStatusTransactionServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                              TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transaction2statusMapper = transaction2statusMapper;
    }

    public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId) {
        TransactionInProgress transactionInProgress= transactionInProgressRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId)));

        if(!transactionInProgress.getMerchantId().equals(merchantId)){
            log.info("Merchant " + merchantId + " not authorized to retrieve transaction " + transactionId);
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId));
        }

        return transaction2statusMapper.transactionInProgressMapper(transactionInProgress);
    }
}
