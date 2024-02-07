package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PreAuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class RewardCalculatorMapper {

    public PreAuthPaymentRequestDTO preAuthRequestMap(TransactionInProgress transactionInProgress) {
        return PreAuthPaymentRequestDTO.builder()
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
    public AuthPaymentRequestDTO authRequestMap(TransactionInProgress transactionInProgress) {
        return AuthPaymentRequestDTO.builder()
                .rewardCents(transactionInProgress.getCounterVersion())
                .build();
    }

    public AuthPaymentDTO rewardResponseMap(AuthPaymentResponseDTO responseDTO, TransactionInProgress transactionInProgress) {
        AuthPaymentDTO out = AuthPaymentDTO.builder()
                .id(responseDTO.getTransactionId())
                .reward(0L)
                .initiativeId(responseDTO.getInitiativeId())
                .rejectionReasons(
                        ObjectUtils.firstNonNull(
                                responseDTO.getRejectionReasons(),
                                Collections.emptyList()))
                .status(responseDTO.getStatus())
                .trxCode(transactionInProgress.getTrxCode())
                .amountCents(responseDTO.getAmountCents())
                .initiativeName(transactionInProgress.getInitiativeName())
                .businessName(transactionInProgress.getBusinessName())
                .trxDate(transactionInProgress.getTrxDate())
                .counterVersion(transactionInProgress.getCounterVersion()).build();

        if (responseDTO.getReward() != null) {
            out.setReward(CommonUtilities.euroToCents(responseDTO.getReward().getAccruedReward()));
            out.setCounters(responseDTO.getReward().getCounters());
            out.setRewards(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()));
        } else {
            out.setRewards(Collections.emptyMap());
        }
        return out;
    }
}
