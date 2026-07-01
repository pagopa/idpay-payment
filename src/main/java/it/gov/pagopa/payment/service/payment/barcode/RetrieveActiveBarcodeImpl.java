package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.payment.utils.RewardConstants.TRX_CHANNEL_BARCODE;

@Slf4j
@Service
public class RetrieveActiveBarcodeImpl implements RetrieveActiveBarcode{
    public static final String NO_ACTIVE_TRANSACTION_FOUND_FOR_USER = "No active transaction found for user";
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;

    public RetrieveActiveBarcodeImpl(TransactionRepository transactionRepository, TransactionInProgressRepository transactionInProgressRepository, TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
    }

    @Override
    public TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId) {
        List<TransactionInProgress> transactions = transactionInProgressRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);
        List<Transaction> trxs = transactionRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);

        if (transactions.isEmpty()) {
            throw new TransactionNotFoundOrExpiredException(NO_ACTIVE_TRANSACTION_FOUND_FOR_USER);
        }
        if (trxs.isEmpty()) {
            throw new TransactionNotFoundOrExpiredException(NO_ACTIVE_TRANSACTION_FOUND_FOR_USER);
        }

        TransactionInProgress latest = null;

        for (TransactionInProgress trx : transactions) {
            if (trx.getStatus() == SyncTrxStatus.AUTHORIZED) {
                return null;
            }

            if (latest == null || trx.getTrxDate().isBefore(latest.getTrxDate())) {
                latest = trx;
            }
        }

        if(null != latest) {
            latest.setAmountCents(latest.getVoucherAmountCents());
        }

        return transactionBarCodeInProgress2TransactionResponseMapper.apply(latest);
    }
}