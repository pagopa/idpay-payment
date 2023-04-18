package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import org.springframework.stereotype.Service;


@Service
public class TransactionInProgress2SyncTrxStatus {
    public SyncTrxStatusDTO transactionInProgressMapper(TransactionInProgress transaction){
        return SyncTrxStatusDTO.builder()
                .id(transaction.getId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxCode(transaction.getTrxCode())
                .trxDate(transaction.getTrxDate())
                .authDate(transaction.getAuthDate())
                .operationType(transaction.getOperationType())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .merchantId(transaction.getMerchantId())
                .initiativeId(transaction.getInitiativeId())
                .rewardCents(Utils.euroToCents(transaction.getReward().getProvidedReward()))
                .rejectionReasons(transaction.getRejectionReasons())
                .status(transaction.getStatus())
                .build();
    }
}
