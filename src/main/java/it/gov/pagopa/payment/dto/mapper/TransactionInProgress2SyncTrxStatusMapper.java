package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;


@Service
public class TransactionInProgress2SyncTrxStatusMapper {

    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    public TransactionInProgress2SyncTrxStatusMapper(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper) {
        this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    }


    public SyncTrxStatusDTO transactionInProgressMapper(TransactionInProgress transaction){

        Pair<Boolean, Long> splitAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getReward());

        SyncTrxStatusDTO response = SyncTrxStatusDTO.builder()
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
                .splitPayment(splitAndResidualAmountCents.getKey())
                .residualAmountCents(splitAndResidualAmountCents.getValue())
                .build();

        if(evaluateTransactionStatusAndChannel(transaction)){
            response.setQrcodePngUrl(transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(transaction.getTrxCode()));
            response.setQrcodeTxtUrl(transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(transaction.getTrxCode()));
        }

        return response;
    }

    private boolean evaluateTransactionStatusAndChannel(TransactionInProgress transaction){
        return (SyncTrxStatus.CREATED.equals(transaction.getStatus()) && !RewardConstants.TRX_CHANNEL_BARCODE.equals(transaction.getChannel()))
                || (!SyncTrxStatus.CREATED.equals(transaction.getStatus()) && RewardConstants.TRX_CHANNEL_QRCODE.equals(transaction.getChannel()));
    }

}
