package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;

public class AuthPaymentRequestMapper {

  public AuthPaymentRequestDTO rewardMap(TransactionInProgress transactionInProgress) {
    return AuthPaymentRequestDTO.builder()
        .transactionId(transactionInProgress.getId())
        .userId(transactionInProgress.getUserId())
        .merchantId(transactionInProgress.getMerchantId())
        .senderCode(transactionInProgress.getSenderCode())
        .merchantFiscalCode(transactionInProgress.getMerchantFiscalCode())
        .vat(transactionInProgress.getVat())
        .idTrxAcquirer(transactionInProgress.getIdTrxAcquirer())
        .trxDate(transactionInProgress.getTrxDate())
        .amountCents(transactionInProgress.getAmountCents())
        .amountCurrency(transactionInProgress.getAmountCurrency())
        .mcc(transactionInProgress.getMcc())
        .acquirerCode(transactionInProgress.getAcquirerCode())
        .acquirerId(transactionInProgress.getAcquirerId())
        .idTrxIssuer(transactionInProgress.getIdTrxIssuer())
        .operationType(transactionInProgress.getOperationType())
        .build();
  }

  public AuthPaymentDTO rewardResponseMap(AuthPaymentResponseDTO responseDTO,
      TransactionInProgress transactionInProgress) {
    return AuthPaymentDTO.builder()
        .id(responseDTO.getTransactionId())
        .reward(responseDTO.getReward())
        .initiativeId(responseDTO.getInitiativeId())
        .rejectReasons(responseDTO.getRejectionReasons())
        .status(responseDTO.getStatus())
        .trxCode(transactionInProgress.getTrxCode())
        .build();
  }

}
