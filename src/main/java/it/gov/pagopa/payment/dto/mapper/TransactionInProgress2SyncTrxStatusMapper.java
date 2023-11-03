package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;


@Service
public class TransactionInProgress2SyncTrxStatusMapper {

    private static final String TRX_CHANNEL_QRCODE = "QRCODE";

    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    public TransactionInProgress2SyncTrxStatusMapper(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper) {
        this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    }


    public SyncTrxStatusDTO transactionInProgressMapper(TransactionInProgress transaction){
        return SyncTrxStatusDTO.builder()
                .id(transaction.getId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxCode(transaction.getTrxCode())
                .trxDate(transaction.getTrxDate())
                .authDate(transaction.getTrxChargeDate())
                .operationType(transaction.getOperationTypeTranscoded())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .merchantId(transaction.getMerchantId())
                .initiativeId(transaction.getInitiativeId())
                .rewardCents(transaction.getReward())
                .rejectionReasons(transaction.getRejectionReasons())
                .status(transaction.getStatus())
                .qrcodePngUrl(TRX_CHANNEL_QRCODE.equals(transaction.getChannel()) ? transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(transaction.getTrxCode()) : null)
                .qrcodeTxtUrl(TRX_CHANNEL_QRCODE.equals(transaction.getChannel()) ? transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(transaction.getTrxCode()) : null)
                .build();
    }
}
