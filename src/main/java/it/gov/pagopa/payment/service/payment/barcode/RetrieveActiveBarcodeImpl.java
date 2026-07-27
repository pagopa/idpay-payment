package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
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
    private final TransactionMapper transactionMapper;

    public RetrieveActiveBarcodeImpl(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);

        if (transactions.isEmpty()) {
            throw new TransactionNotFoundOrExpiredException(NO_ACTIVE_TRANSACTION_FOUND_FOR_USER);
        }

        Transaction latest = null;

        for (Transaction trx : transactions) {
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

        return transactionMapper.transactionBarCodeToTransactionResponse(latest);
    }
}