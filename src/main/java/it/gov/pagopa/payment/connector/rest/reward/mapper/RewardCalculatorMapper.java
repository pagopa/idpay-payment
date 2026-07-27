package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

@Service
public class RewardCalculatorMapper {

    public PaymentRequestDTO preAuthRequestMap(Transaction transaction) {
        return PaymentRequestDTO.builder()
                .transactionId(transaction.getId())
                .userId(transaction.getUserId())
                .merchantId(transaction.getMerchantId())
                .merchantFiscalCode(transaction.getMerchantFiscalCode())
                .vat(transaction.getVat())
                .idTrxAcquirer(transaction.getIdTrxAcquirer())
                .trxDate(transaction.getTrxDate().atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxChargeDate(transaction.getTrxChargeDate().atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime())
                .channel(transaction.getChannel())
                .voucherAmountCents(transaction.getVoucherAmountCents())
                .productType(transaction.getProductType())
                .build();
    }
    public AuthPaymentRequestDTO authRequestMap(Transaction transaction) {
        return AuthPaymentRequestDTO.builder()
                .transactionId(transaction.getId())
                .userId(transaction.getUserId())
                .merchantId(transaction.getMerchantId())
                .merchantFiscalCode(transaction.getMerchantFiscalCode())
                .vat(transaction.getVat())
                .idTrxAcquirer(transaction.getIdTrxAcquirer())
                .trxDate(transaction.getTrxDate().atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxChargeDate(transaction.getTrxChargeDate().atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime())
                .channel(transaction.getChannel())
                .rewardCents(transaction.getRewardCents())
                .voucherAmountCents(transaction.getVoucherAmountCents())
                .productType(transaction.getProductType())
                .build();
    }

    public AuthPaymentDTO rewardResponseMap(AuthPaymentResponseDTO responseDTO, Transaction transaction) {
        AuthPaymentDTO out = AuthPaymentDTO.builder()
                .id(responseDTO.getTransactionId())
                .rewardCents(0L)
                .initiativeId(responseDTO.getInitiativeId())
                .rejectionReasons(
                        ObjectUtils.firstNonNull(
                                responseDTO.getRejectionReasons(),
                                Collections.emptyList()))
                .status(responseDTO.getStatus())
                .trxCode(transaction.getTrxCode())
                .amountCents(responseDTO.getAmountCents())
                .initiativeName(transaction.getInitiativeName())
                .businessName(transaction.getBusinessName())
                .trxDate(transaction.getTrxDate())
                .counterVersion(responseDTO.getCounterVersion())
                .additionalProperties(transaction.getAdditionalProperties())
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