package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CommonStatusTransactionServiceImpl {
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper;

    public CommonStatusTransactionServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                              TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transaction2statusMapper = transaction2statusMapper;
    }

    public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId, String acquirerId) {
        TransactionInProgress transactionInProgress= transactionInProgressRepository.findByIdAndMerchantIdAndAcquirerId(transactionId, merchantId, acquirerId)
                .orElseThrow(() -> new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

        return transaction2statusMapper.transactionInProgressMapper(transactionInProgress);
    }
}
