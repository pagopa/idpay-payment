package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentMapper {

  public AuthPaymentDTO transactionMapper(TransactionInProgress transaction) {
    AuthPaymentDTO authPaymentDTO= AuthPaymentDTO.builder()
        .id(transaction.getId())
        .reward(transaction.getReward())
        .initiativeId(transaction.getInitiativeId())
        .rejectionReasons(transaction.getRejectionReasons())
        .status(transaction.getStatus())
        .trxCode(transaction.getTrxCode())
        .amountCents(transaction.getAmountCents())
        .build();
    residualAmountCentsCalculator(authPaymentDTO);
    return authPaymentDTO;
  }

  public static void residualAmountCentsCalculator(AuthPaymentDTO authPaymentDTO){
      if(authPaymentDTO.getAmountCents() != null && authPaymentDTO.getReward() != null) {
          authPaymentDTO.setResidualAmountCents(authPaymentDTO.getAmountCents() - authPaymentDTO.getReward());
          authPaymentDTO.setSplitPayment(authPaymentDTO.getReward().equals(authPaymentDTO.getAmountCents()) ? Boolean.FALSE : Boolean.TRUE);
      }
  }
}
