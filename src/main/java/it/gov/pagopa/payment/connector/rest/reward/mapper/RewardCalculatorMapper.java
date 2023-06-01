package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RewardCalculatorMapper {

  public AuthPaymentRequestDTO rewardMap(TransactionInProgress transactionInProgress) {
    return AuthPaymentRequestDTO.builder()
        .transactionId(transactionInProgress.getId())
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
        .trxChargeDate(transactionInProgress.getTrxChargeDate())
        .channel(transactionInProgress.getChannel())
        .build();
  }

  public AuthPaymentDTO rewardResponseMap(
      AuthPaymentResponseDTO responseDTO, TransactionInProgress transactionInProgress) {
    transactionInProgress.getRewards().put(responseDTO.getInitiativeId(), responseDTO.getReward());
    return AuthPaymentDTO.builder()
        .id(responseDTO.getTransactionId())
        .reward(
            responseDTO.getReward() != null
                ? CommonUtilities.euroToCents(responseDTO.getReward().getAccruedReward())
                : 0L)
        .initiativeId(responseDTO.getInitiativeId())
        .rejectionReasons(
                ObjectUtils.firstNonNull(
                        responseDTO.getRejectionReasons(),
                        Collections.emptyList()))
        .status(responseDTO.getStatus())
        .trxCode(transactionInProgress.getTrxCode())
        .amountCents(responseDTO.getAmountCents())
        .build();
  }
}
