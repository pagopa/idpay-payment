package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class AuthPaymentMapper {

  public AuthPaymentDTO transactionMapper(TransactionInProgress transaction) {
    return AuthPaymentDTO.builder()
            .id(transaction.getId())
            .reward(transaction.getReward())
            .initiativeId(transaction.getInitiativeId())
            .initiativeName(transaction.getInitiativeName())
            .businessName(transaction.getBusinessName())
            .rejectionReasons(transaction.getRejectionReasons())
            .status(transaction.getStatus())
            .trxCode(transaction.getTrxCode())
            .trxDate(transaction.getTrxDate())
            .amountCents(transaction.getAmountCents())
            .counters(Optional.ofNullable(transaction.getRewards()).flatMap(r -> r.values().stream().map(Reward::getCounters).filter(Objects::nonNull).findFirst()).orElse(null))
            .rewards(transaction.getRewards())
            .counterVersion(transaction.getCounterVersion())
            .build();
  }
}
