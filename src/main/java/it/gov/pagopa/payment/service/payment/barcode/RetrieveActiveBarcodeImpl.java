package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.payment.utils.RewardConstants.TRX_CHANNEL_BARCODE;

@Slf4j
@Service
public class RetrieveActiveBarcodeImpl implements RetrieveActiveBarcode{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;

    public RetrieveActiveBarcodeImpl(TransactionInProgressRepository transactionInProgressRepository, TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
    }

    @Override
    public TransactionBarCodeResponse findOldestOrAuthorized(String userId, String initiativeId) {
        List<TransactionInProgress> transactions = transactionInProgressRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);
        if (transactions.isEmpty()) {
            return null;
        }

        if (transactions.size() == 1) {
            return transactionBarCodeInProgress2TransactionResponseMapper.apply(transactions.getFirst());
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

        return transactionBarCodeInProgress2TransactionResponseMapper.apply(latest);
    }
}
