package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.connector.event.trx.RewardTransactionMapper;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionConflictException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class InvoiceTransactionRepositoryImpl implements InvoiceTransactionRepository {

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
                .setParameter("transactionId", command.transactionId())
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
        Instant occurredAt = (Instant) eventMetadata[1];

        ObjectNode payload = objectMapper.valueToTree(
                rewardTransactionMapper.transactionToRewardTransaction(updatedTransaction));
        payload.put("eventId", Long.toString(eventId));
        payload.put("schemaVersion", 1);
        payload.put("eventType", command.eventType().name());
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
                        """)
                .setParameter("eventId", eventId)
                .setParameter("transactionId", updatedTransaction.getId())
                .setParameter("userId", updatedTransaction.getUserId())
                .setParameter("eventType", command.eventType().name())
                .setParameter("payload", objectMapper.writeValueAsString(payload))
                .setParameter("occurredAt", occurredAt)
                .setParameter("transactionRevision", updatedTransaction.getTransactionRevision())
                .executeUpdate();

        return updatedTransaction;
    }
}
