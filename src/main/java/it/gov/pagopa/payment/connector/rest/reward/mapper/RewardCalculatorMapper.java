package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorMapper {

  public AuthPaymentRequestDTO rewardMap(TransactionInProgress transactionInProgress) {
    return AuthPaymentRequestDTO.builder()
        .transactionId(transactionInProgress.getId())
        .correlationId(transactionInProgress.getCorrelationId())
        .userId(transactionInProgress.getUserId())
        .merchantId(transactionInProgress.getMerchantId())
        .merchantFiscalCode(transactionInProgress.getMerchantFiscalCode())
        .vat(transactionInProgress.getVat())
        .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
        .trxDate(transactionInProgress.getTrxDate())
        .amountCents(transactionInProgress.getAmountCents())
        .amountCurrency(transactionInProgress.getAmountCurrency())
        .mcc(transactionInProgress.getMcc())
        .acquirerId(transactionInProgress.getAcquirerId())
        .idTrxIssuer(transactionInProgress.getIdTrxIssuer())
        .operationType(transactionInProgress.getOperationTypeTranscoded())
        .trxChargeDate(transactionInProgress.getTrxChargeDate())
        .correlationId(transactionInProgress.getCorrelationId())
        .channel(transactionInProgress.getChannel())
        .build();
  }

  public AuthPaymentDTO rewardResponseMap(
      AuthPaymentResponseDTO responseDTO, TransactionInProgress transactionInProgress) {
    return AuthPaymentDTO.builder()
        .id(responseDTO.getTransactionId())
        .reward(
            responseDTO.getReward() != null
                ? Utils.euroToCents(responseDTO.getReward().getAccruedReward())
                : 0L)
        .initiativeId(responseDTO.getInitiativeId())
        .rejectionReasons(responseDTO.getRejectionReasons())
        .status(responseDTO.getStatus())
        .trxCode(transactionInProgress.getTrxCode())
        .amountCents(responseDTO.getAmountCents())
        .build();
  }
}
