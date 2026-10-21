package it.gov.pagopa.payment.connector.event.trx;

import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RewardTransactionMapper {

    public RewardTransactionDTO transactionToRewardTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        List<String> initiatives = transaction.getInitiatives();
        if (initiatives == null || initiatives.size() != 1
                || !Objects.equals(transaction.getInitiativeId(), initiatives.getFirst())) {
            throw new IllegalArgumentException("A generic transaction snapshot must contain exactly one initiative");
        }

        return RewardTransactionDTO.builder()
                .id(transaction.getId())
                .trxCode(transaction.getTrxCode())
                .operationType(transaction.getOperationType())
                .operationTypeTranscoded(transaction.getOperationTypeTranscoded())
                .status(transaction.getStatus())
                .trxDate(transaction.getTrxDate())
                .trxChargeDate(transaction.getTrxChargeDate())
                .elaborationDateTime(transaction.getElaborationDateTime())
                .updateDate(transaction.getUpdateDate())
                .userId(transaction.getUserId())
                .merchantId(transaction.getMerchantId())
                .acquirerId(transaction.getAcquirerId())
                .pointOfSaleId(transaction.getPointOfSaleId())
                .amountCents(transaction.getAmountCents())
                .effectiveAmountCents(transaction.getEffectiveAmountCents())
                .voucherAmountCents(transaction.getVoucherAmountCents())
                .amountCurrency(transaction.getAmountCurrency())
                .channel(transaction.getChannel())
                .initiativeId(transaction.getInitiativeId())
                .initiativeName(transaction.getInitiativeName())
                .initiatives(List.copyOf(initiatives))
                .businessName(transaction.getBusinessName())
                .franchiseName(transaction.getFranchiseName())
                .invoiceData(transaction.getInvoiceData())
                .creditNoteData(transaction.getCreditNoteData())
                .correlationId(transaction.getCorrelationId())
                .createdAt(transaction.getCreatedAt())
                .idTrxAcquirer(transaction.getIdTrxAcquirer())
                .merchantFiscalCode(transaction.getMerchantFiscalCode())
                .vat(transaction.getVat())
                .pointOfSaleType(transaction.getPointOfSaleType())
                .productType(transaction.getProductType())
                .familyId(transaction.getFamilyId())
                .rewardCents(transaction.getRewardCents())
                .counterVersion(transaction.getCounterVersion())
                .transactionRevision(transaction.getTransactionRevision() == null ? 0L : transaction.getTransactionRevision())
                .rewards(transaction.getRewards())
                .rejectionReasons(transaction.getRejectionReasons())
                .initiativeRejectionReasons(transaction.getInitiativeRejectionReasons())
                .additionalProperties(transaction.getAdditionalProperties())
                .mcc(transaction.getMcc())
                .idTrxIssuer(transaction.getIdTrxIssuer())
                .trxEndDate(transaction.getTrxEndDate())
                .extendedAuthorization(transaction.getExtendedAuthorization())
                .productName(transaction.getProductName())
                .build();
    }
}
