package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionOutcomeDTO;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class TransactionInProgress2TransactionOutcomeDTOMapper implements Function<TransactionInProgress, TransactionOutcomeDTO> {

  @Override
  public TransactionOutcomeDTO apply(TransactionInProgress trx) {
      List<String> rejectionReasons = Collections.emptyList();
      Map<String, List<String>> initiativeRejectionReasons;

      if(!CollectionUtils.isEmpty(trx.getRejectionReasons())){
          if(trx.getRejectionReasons().contains(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)){
              rejectionReasons = List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE);
          }
          initiativeRejectionReasons = Map.of(
                  trx.getInitiativeId(),
                  trx.getRejectionReasons()
          );
      } else {
          initiativeRejectionReasons = Collections.emptyMap();
      }

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
        .reward(trx.getReward())
        .rewards(trx.getRewards())
        .rejectionReasons(rejectionReasons)
        .initiativeRejectionReasons(initiativeRejectionReasons)
        .userId(trx.getUserId())
        .status(trx.getStatus())
        .channel(trx.getChannel())
        .build();
  }
}
