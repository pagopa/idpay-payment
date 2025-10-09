package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class RewardCalculatorMapper {

    public PaymentRequestDTO preAuthRequestMap(TransactionInProgress transactionInProgress) {
        return PaymentRequestDTO.builder()
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
                .voucherAmountCents(transactionInProgress.getVoucherAmountCents())
                .build();
    }
    public AuthPaymentRequestDTO authRequestMap(TransactionInProgress transactionInProgress) {
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
                .rewardCents(transactionInProgress.getRewardCents())
                .voucherAmountCents(transactionInProgress.getVoucherAmountCents())
                .build();
    }

    public AuthPaymentDTO rewardResponseMap(AuthPaymentResponseDTO responseDTO, TransactionInProgress transactionInProgress) {
        AuthPaymentDTO out = AuthPaymentDTO.builder()
                .id(responseDTO.getTransactionId())
                .rewardCents(0L)
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
                .counterVersion(responseDTO.getCounterVersion())
                .additionalProperties(transactionInProgress.getAdditionalProperties())
                .build();

        if (responseDTO.getReward() != null) {
            out.setRewardCents(responseDTO.getReward().getAccruedRewardCents());
            out.setCounters(responseDTO.getReward().getCounters());
            out.setRewards(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()));
        } else {
            out.setRewards(Collections.emptyMap());
        }

        return out;
    }
}
