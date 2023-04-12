package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TransactionInProgress2TransactionMapper implements Function<TransactionInProgress, TransactionDTO> {

    @Override
    public TransactionDTO apply(TransactionInProgress transactionInProgress) {
        return TransactionDTO.builder()
                .id(transactionInProgress.getId())
                .trxCode(transactionInProgress.getTrxCode())
                .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
                .acquirerCode(transactionInProgress.getAcquirerCode())
                .trxDate(transactionInProgress.getTrxDate())
                .trxChargeDate(transactionInProgress.getTrxChargeDate())
                .authDate(transactionInProgress.getAuthDate())
                .elaborationDateTime(transactionInProgress.getElaborationDateTime())
                .hpan(transactionInProgress.getHpan())
                .operationType(transactionInProgress.getOperationType())
                .operationTypeTranscoded(transactionInProgress.getOperationTypeTranscoded())
                .idTrxIssuer(transactionInProgress.getIdTrxIssuer())
                .correlationId(transactionInProgress.getCorrelationId())
                .amountCents(transactionInProgress.getAmountCents())
                .effectiveAmount(transactionInProgress.getEffectiveAmount())
                .amountCurrency(transactionInProgress.getAmountCurrency())
                .mcc(transactionInProgress.getMcc())
                .acquirerId(transactionInProgress.getAcquirerId())
                .merchantId(transactionInProgress.getMerchantId())
                .senderCode(transactionInProgress.getSenderCode())
                .merchantFiscalCode(transactionInProgress.getMerchantFiscalCode())
                .vat(transactionInProgress.getVat())
                .initiativeId(transactionInProgress.getInitiativeId())
                .userId(transactionInProgress.getUserId())
                .status(transactionInProgress.getStatus())
                .callbackUrl(transactionInProgress.getCallbackUrl())
                .build();
    }
}
