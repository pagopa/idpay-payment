package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;

    public RetrieveActiveBarcodeImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
    }

    @Override
    public TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId) {
        List<TransactionInProgress> mongo = transactionInProgressRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);
        if (mongo.isEmpty()) {
            return null;
        }

        TransactionInProgress latestMongo = null;

        for (TransactionInProgress trx : mongo) {
            if (trx.getStatus() == SyncTrxStatus.AUTHORIZED) {
                return null;
            }

            if (latestMongo == null || trx.getTrxDate().isBefore(latestMongo.getTrxDate())) {
                latestMongo = trx;
            }
        }

        if(null != latestMongo) {
            latestMongo.setAmountCents(latestMongo.getVoucherAmountCents());
        }


        List<Transaction> postgres = transactionRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);
        if (postgres.isEmpty()) {
            return null;
        }

        Transaction latestPostgres = null;

        for (Transaction trx : postgres) {
            if (trx.getStatus() == SyncTrxStatus.AUTHORIZED) {
                return null;
            }

            if (latestPostgres == null || trx.getTrxDate().isBefore(latestPostgres.getTrxDate())) {
                latestPostgres = trx;
            }
        }

        if(null != latestPostgres) {
            latestPostgres.setAmountCents(latestPostgres.getVoucherAmountCents());
        }

        return transactionBarCodeInProgress2TransactionResponseMapper.apply(latestMongo);
    }
}
