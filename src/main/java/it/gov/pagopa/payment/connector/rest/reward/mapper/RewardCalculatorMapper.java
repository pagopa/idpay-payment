package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

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
                .trxDate(transaction.getTrxDate())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxChargeDate(transaction.getTrxChargeDate())
                .channel(transaction.getChannel())
                .voucherAmountCents(transaction.getVoucherAmountCents())
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
                .trxDate(transaction.getTrxDate())
                .amountCents(transaction.getAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .mcc(transaction.getMcc())
                .acquirerId(transaction.getAcquirerId())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxChargeDate(transaction.getTrxChargeDate())
                .channel(transaction.getChannel())
                .rewardCents(transaction.getRewardCents())
                .voucherAmountCents(transaction.getVoucherAmountCents())
                .build();
    }

    public AuthPaymentDTO rewardResponseMap(
            AuthPaymentResponseDTO responseDTO,
            Transaction transaction) {

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
            out.setRewards(
                    Map.of(
                            responseDTO.getInitiativeId(),
                            responseDTO.getReward()));
        } else {
            out.setRewards(Collections.emptyMap());
        }

        return out;
    }

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
