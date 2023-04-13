package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentMapper {

  public AuthPaymentRequestDTO rewardMap(TransactionInProgress transactionInProgress) {
    return AuthPaymentRequestDTO.builder()
        .transactionId(transactionInProgress.getId())
        .correlationId(transactionInProgress.getCorrelationId())
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
        .operationType(transactionInProgress.getOperationTypeTranscoded())
        .trxChargeDate(transactionInProgress.getTrxChargeDate())
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

  public AuthPaymentDTO trxIdempotence(TransactionInProgress transaction){
    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .reward(transaction.getReward())
        .initiativeId(transaction.getInitiativeId())
        .rejectReasons(transaction.getRejectionReasons())
        .status(transaction.getStatus())
        .trxCode(transaction.getTrxCode())
        .build();
  }

}
