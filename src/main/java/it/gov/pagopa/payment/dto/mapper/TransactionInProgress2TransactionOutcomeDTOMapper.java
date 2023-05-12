package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionOutcomeDTO;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TransactionInProgress2TransactionOutcomeDTOMapper implements Function<TransactionInProgress, TransactionOutcomeDTO> {

  @Override
  public TransactionOutcomeDTO apply(TransactionInProgress trx) {
    return TransactionOutcomeDTO.builder()
        .id(trx.getId())
        .trxCode(trx.getTrxCode())
        .idTrxAcquirer(trx.getIdTrxAcquirer())
        .trxDate(trx.getTrxDate())
        .trxChargeDate(trx.getTrxChargeDate())
        .authDate(trx.getAuthDate())
        .elaborationDateTime(trx.getElaborationDateTime())
        .operationType(trx.getOperationType())
        .operationTypeTranscoded(trx.getOperationTypeTranscoded())
        .idTrxIssuer(trx.getIdTrxIssuer())
        .correlationId(trx.getCorrelationId())
        .amountCents(trx.getAmountCents())
        .effectiveAmount(trx.getEffectiveAmount())
        .amountCurrency(trx.getAmountCurrency())
        .mcc(trx.getMcc())
        .acquirerId(trx.getAcquirerId())
        .merchantId(trx.getMerchantId())
        .merchantFiscalCode(trx.getMerchantFiscalCode())
        .vat(trx.getVat())
        .initiativeId(trx.getInitiativeId())
        .rewards(trx.getRewards())
        .rejectionReasons(trx.getRejectionReasons())
        .initiativeRejectionReasons(Map.of(
            trx.getInitiativeId(),
            Collections.emptyList()
        ))
        .userId(trx.getUserId())
        .status(trx.getStatus())
        .channel(trx.getChannel())
        .build();
  }
}
