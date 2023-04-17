package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentMapper {

  public AuthPaymentDTO transactionMapper(TransactionInProgress transaction) {
    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .reward(transaction.getReward())
        .initiativeId(transaction.getInitiativeId())
        .rejectionReasons(transaction.getRejectionReasons())
        .status(transaction.getStatus())
        .trxCode(transaction.getTrxCode())
        .amountCents(Utils.euroToCents(transaction.getEffectiveAmount()))
        .build();
  }

}
