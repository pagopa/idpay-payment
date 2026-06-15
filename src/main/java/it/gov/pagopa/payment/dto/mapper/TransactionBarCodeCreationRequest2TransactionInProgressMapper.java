package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionBarCodeCreationRequest2TransactionInProgressMapper {

    public Transaction toPostgres(TransactionInProgress trx, String trxCode) {

        if (trx == null) {
            return null;
        }

        return Transaction.builder()
                // -------------------------
                // IDENTIFIERS
                // -------------------------
                .id(trx.getId())
                .trxCode(trxCode != null ? trxCode : trx.getTrxCode())
                .correlationId(trx.getCorrelationId())

                // -------------------------
                // STATUS / FLOW
                // -------------------------
                .status(trx.getStatus())
                .operationType(trx.getOperationType())
                .operationTypeTranscoded(trx.getOperationTypeTranscoded())

                // -------------------------
                // DATES
                // -------------------------
                .trxDate(trx.getTrxDate())
                .trxChargeDate(trx.getTrxChargeDate())
                .elaborationDate(trx.getElaborationDateTime())
                .updateDate(trx.getUpdateDate())
                .trxEndDate(trx.getTrxEndDate())
                .createdAt(trx.getElaborationDateTime() != null
                        ? trx.getElaborationDateTime()
                        : null)

                // -------------------------
                // USER / MERCHANT
                // -------------------------
                .userId(trx.getUserId())
                .merchantId(trx.getMerchantId())
                .acquirerId(trx.getAcquirerId())
                .pointOfSaleId(trx.getPointOfSaleId())

                // -------------------------
                // AMOUNTS
                // -------------------------
                .amountCents(trx.getAmountCents())
                .effectiveAmountCents(trx.getEffectiveAmountCents())
                .voucherAmountCents(trx.getVoucherAmountCents())
                .amountCurrency(trx.getAmountCurrency())

                // -------------------------
                // INITIATIVE
                // -------------------------
                .initiativeId(trx.getInitiativeId())
                .initiativeName(trx.getInitiativeName())
                .initiatives(trx.getInitiatives())

                // -------------------------
                // MERCHANT INFO
                // -------------------------
                .businessName(trx.getBusinessName())
                .franchiseName(trx.getFranchiseName())
                .merchantFiscalCode(trx.getMerchantFiscalCode())
                .vat(trx.getVat())
                .pointOfSaleType(trx.getPointOfSaleType())

                // -------------------------
                // REWARDING
                // -------------------------
                .rewardCents(trx.getRewardCents())
                .counterVersion(trx.getCounterVersion())
                .rewards(trx.getRewards())
                .rejectionReasons(trx.getRejectionReasons())
                .initiativeRejectionReasons(trx.getInitiativeRejectionReasons())

                // -------------------------
                // ADDITIONAL
                // -------------------------
                .additionalProperties(trx.getAdditionalProperties())

                // -------------------------
                // TECH / EXTRA FIELDS
                // -------------------------
                .mcc(trx.getMcc())
                .idTrxAcquirer(trx.getIdTrxAcquirer())
                .idTrxIssuer(trx.getIdTrxIssuer())
                .extendedAuthorization(trx.getExtendedAuthorization())

                .build();
    }

    public TransactionInProgress apply(
            TransactionBarCodeCreationRequest transactionBarCodeCreationRequest,
            String channel,
            String userId,
            String initiativeName,
            Map<String, String> additionalProperties,
            boolean extendedAuthorization,
            OffsetDateTime trxEndDate
    ) {
        String id =
                "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());

        OffsetDateTime now = OffsetDateTime.now();

        return TransactionInProgress.builder()
                .id(id)
                .correlationId(id)
                .initiativeId(transactionBarCodeCreationRequest.getInitiativeId())
                .initiatives(List.of(transactionBarCodeCreationRequest.getInitiativeId()))
                .initiativeName(initiativeName)
                .trxDate(now)
                .status(SyncTrxStatus.CREATED)
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .channel(channel)
                .userId(userId)
                .updateDate(now.toLocalDateTime())
                .additionalProperties(additionalProperties)
                .extendedAuthorization(extendedAuthorization)
                .trxEndDate(trxEndDate)
                .voucherAmountCents(transactionBarCodeCreationRequest.getVoucherAmountCents())
                .build();
    }

    public Transaction applyTransaction(
            TransactionBarCodeCreationRequest transactionBarCodeCreationRequest,
            String channel,
            String userId,
            String initiativeName,
            Map<String, String> additionalProperties,
            boolean extendedAuthorization,
            OffsetDateTime trxEndDate
    ) {
        String id =
                "%s_%s_%d".formatted(UUID.randomUUID().toString(), channel, System.currentTimeMillis());

        OffsetDateTime now = OffsetDateTime.now();

        return Transaction.builder()
                .id(id)
                .correlationId(id)
                .initiativeId(transactionBarCodeCreationRequest.getInitiativeId())
                .initiatives(List.of(transactionBarCodeCreationRequest.getInitiativeId()))
                .initiativeName(initiativeName)
                .trxDate(now)
                .status(SyncTrxStatus.CREATED)
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .channel(channel)
                .userId(userId)
                .updateDate(now.toLocalDateTime())
                .additionalProperties(additionalProperties)
                .extendedAuthorization(extendedAuthorization)
                .trxEndDate(trxEndDate)
                .voucherAmountCents(transactionBarCodeCreationRequest.getVoucherAmountCents())
                .build();
    }
}
