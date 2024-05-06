package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.AuthPaymentIdpayCodeDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthPaymentIdpayCodeMapper {
    public AuthPaymentIdpayCodeDTO authPaymentMapper(AuthPaymentDTO authPaymentDTO, String secondFactor) {
        return AuthPaymentIdpayCodeDTO.builder()
                .id(authPaymentDTO.getId())
                .rewardCents(authPaymentDTO.getRewardCents())
                .initiativeId(authPaymentDTO.getInitiativeId())
                .initiativeName(authPaymentDTO.getInitiativeName())
                .businessName(authPaymentDTO.getBusinessName())
                .rejectionReasons(authPaymentDTO.getRejectionReasons())
                .status(authPaymentDTO.getStatus())
                .trxCode(authPaymentDTO.getTrxCode())
                .trxDate(authPaymentDTO.getTrxDate())
                .amountCents(authPaymentDTO.getAmountCents())
                .counters(authPaymentDTO.getCounters())
                .rewards(authPaymentDTO.getRewards())
                .residualBudgetCents(authPaymentDTO.getResidualBudgetCents())
                .secondFactor(secondFactor)
                .counterVersion(authPaymentDTO.getCounterVersion())
                .build();
    }
}
