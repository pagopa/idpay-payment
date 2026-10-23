package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.connector.event.trx.RewardTransactionMapper;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionEventType;
import it.gov.pagopa.payment.exception.custom.TransactionConflictException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
public class InvoiceTransactionRepositoryImpl implements InvoiceTransactionRepository {

    private static final String PARAM_TRANSACTION_ID = "transactionId";

    private final EntityManager entityManager;
    private final RewardTransactionMapper rewardTransactionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Transaction updateInvoiceAndCreateEvent(InvoiceTransactionCommand command) {
        String invoiceData = objectMapper.writeValueAsString(command.invoiceData());
        int updatedRows = entityManager.createNativeQuery("""
                        UPDATE "idpay-pagamenti".transaction
                        SET status = 'INVOICED',
                            "invoiceData" = CAST(:invoiceData AS jsonb),
                            "updateDate" = :updateDate,
                            "franchiseName" = COALESCE(CAST(:franchiseName AS varchar), "franchiseName"),
                            "pointOfSaleType" = COALESCE(CAST(:pointOfSaleType AS varchar), "pointOfSaleType"),
                            "businessName" = COALESCE(CAST(:businessName AS varchar), "businessName"),
                            "merchantFiscalCode" = COALESCE(CAST(:merchantFiscalCode AS varchar), "merchantFiscalCode"),
                            "transactionRevision" = "transactionRevision" + 1
                        WHERE id = :transactionId
                          AND "initiativeId" = :initiativeId
                          AND "merchantId" = :merchantId
                          AND jsonb_typeof(initiatives) = 'array'
                          AND jsonb_array_length(initiatives) = 1
                          AND initiatives ->> 0 = :initiativeId
                          AND status = :expectedStatus
                          AND "transactionRevision" = :expectedRevision
                        """)
                .setParameter("invoiceData", invoiceData)
                .setParameter("updateDate", command.updateDate())
                .setParameter("franchiseName", command.franchiseName())
                .setParameter("pointOfSaleType", command.pointOfSaleType())
                .setParameter("businessName", command.businessName())
                .setParameter("merchantFiscalCode", command.merchantFiscalCode())
                .setParameter(PARAM_TRANSACTION_ID, command.transactionId())
                .setParameter("initiativeId", command.initiativeId())
                .setParameter("merchantId", command.merchantId())
                .setParameter("expectedStatus", command.expectedStatus().name())
                .setParameter("expectedRevision", command.expectedRevision())
                .executeUpdate();

        if (updatedRows != 1) {
            throw new TransactionConflictException(
                    ExceptionCode.TRANSACTION_CONFLICT,
                    "Transaction [%s] changed while the invoice operation was in progress"
                            .formatted(command.transactionId()));
        }

        Transaction updatedTransaction = entityManager.find(Transaction.class, command.transactionId());
        entityManager.refresh(updatedTransaction);

        appendOutboxEvent(updatedTransaction, command.eventType());

        return updatedTransaction;
    }

    @Override
    @Transactional
    public Transaction updateStatusAndCreateEvent(
            String transactionId,
            SyncTrxStatus newStatus,
            TransactionEventType eventType,
            LocalDateTime updateDate) {
        int updatedRows = entityManager.createNativeQuery("""
                        UPDATE "idpay-pagamenti".transaction
                        SET status = :newStatus,
                            "updateDate" = :updateDate,
                            "transactionRevision" = COALESCE("transactionRevision", 0) + 1
                        WHERE id = :transactionId
                        """)
                .setParameter("newStatus", newStatus.name())
                .setParameter("updateDate", updateDate)
                .setParameter(PARAM_TRANSACTION_ID, transactionId)
                .executeUpdate();

        if (updatedRows != 1) {
            // Unknown id: skip silently so a single missing row does not fail the whole sync.
            return null;
        }

        Transaction updatedTransaction = entityManager.find(Transaction.class, transactionId);
        entityManager.refresh(updatedTransaction);

        appendOutboxEvent(updatedTransaction, eventType);

        return updatedTransaction;
    }

    private void appendOutboxEvent(Transaction updatedTransaction, TransactionEventType eventType) {
        Object[] eventMetadata = (Object[]) entityManager.createNativeQuery("""
                        SELECT nextval(
                                   pg_get_serial_sequence(
                                       '"idpay-pagamenti".transaction_outbox',
                                       'id'
                                   )
                               ),
                               clock_timestamp()
                        """)
                .getSingleResult();
        long eventId = ((Number) eventMetadata[0]).longValue();
        OffsetDateTime occurredAt =
                ((Instant) eventMetadata[1]).atOffset(ZoneOffset.UTC);

        ObjectNode payload = objectMapper.valueToTree(
                rewardTransactionMapper.transactionToRewardTransaction(updatedTransaction));
        payload.put("eventId", Long.toString(eventId));
        payload.put("schemaVersion", 1);
        payload.put("eventType", eventType.name());
        payload.put("occurredAt", occurredAt.toString());
        payload.put("transactionRevision", updatedTransaction.getTransactionRevision());

        entityManager.createNativeQuery("""
                        INSERT INTO "idpay-pagamenti".transaction_outbox (
                            id,
                            transaction_id,
                            user_id,
                            event_type,
                            payload,
                            created_at,
                            transaction_revision,
                            schema_version,
                            occurred_at
                        )
                        OVERRIDING SYSTEM VALUE
                        VALUES (
                            :eventId,
                            :transactionId,
                            :userId,
                            :eventType,
                            CAST(:payload AS jsonb),
                            :occurredAt,
                            :transactionRevision,
                            1,
                            :occurredAt
                        )
                        ON CONFLICT (transaction_id, transaction_revision) DO NOTHING
                        """)
                .setParameter("eventId", eventId)
                .setParameter(PARAM_TRANSACTION_ID, updatedTransaction.getId())
                .setParameter("userId", updatedTransaction.getUserId())
                .setParameter("eventType", eventType.name())
                .setParameter("payload", objectMapper.writeValueAsString(payload))
                .setParameter("occurredAt", occurredAt)
                .setParameter("transactionRevision", updatedTransaction.getTransactionRevision())
                .executeUpdate();
    }
}
