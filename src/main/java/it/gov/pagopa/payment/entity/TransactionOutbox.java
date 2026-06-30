package it.gov.pagopa.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction_outbox")
public class TransactionOutbox {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "payload", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}