package it.gov.pagopa.payment.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TransactionOutboxMigrationTest {

    private static final String SCHEMA = "\"idpay-pagamenti\"";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @BeforeEach
    void migrateSchema() {
        flyway().clean();
        flyway().migrate();
    }

    @Test
    void shouldLeaveInvoiceEventsToExplicitPersistenceAndKeepOtherStatusEvents() throws SQLException {
        execute("""
                INSERT INTO %s.transaction (
                    id, "trxCode", "operationType", status, "trxDate", "userId",
                    "amountCents", "transactionRevision"
                ) VALUES (
                    'trx-1', 'code-1', '00', 'CAPTURED', now(), 'user-1', 100, 0
                )
                """.formatted(SCHEMA));

        execute("""
                UPDATE %s.transaction
                SET status = 'INVOICED', "transactionRevision" = 1
                WHERE id = 'trx-1'
                """.formatted(SCHEMA));
        execute("""
                UPDATE %s.transaction
                SET status = 'REFUNDED',
                    "transactionRevision" = 2
                WHERE id = 'trx-1'
                """.formatted(SCHEMA));

        assertEquals(2, queryLong("""
                SELECT count(*)
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-1'
                """.formatted(SCHEMA)));
        assertEquals(0, queryLong("""
                SELECT count(*)
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-1'
                  AND event_type = 'TRANSACTION_INVOICED'
                """.formatted(SCHEMA)));
        assertEquals(1, queryLong("""
                SELECT count(*)
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-1'
                  AND event_type = 'TRANSACTION_REFUNDED'
                  AND transaction_revision = 2
                """.formatted(SCHEMA)));
    }

    @Test
    void shouldNotMutateExistingRowForDuplicateRevision() throws SQLException {
        execute("""
                INSERT INTO %s.transaction (
                    id, "trxCode", "operationType", status, "trxDate", "userId",
                    "amountCents", "transactionRevision", "invoiceData"
                ) VALUES (
                    'trx-duplicate', 'code-duplicate', '00', 'INVOICED', now(),
                    'user-duplicate', 100, 4, '{"documentNumber":"invoice-1"}'
                )
                """.formatted(SCHEMA));

        String originalPayload = queryString("""
                SELECT payload::TEXT
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-duplicate'
                  AND transaction_revision = 4
                """.formatted(SCHEMA));
        OffsetDateTime originalCreatedAt = queryOffsetDateTime("""
                SELECT created_at
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-duplicate'
                  AND transaction_revision = 4
                """.formatted(SCHEMA));

        execute("""
                UPDATE %s.transaction
                SET "invoiceData" = '{"documentNumber":"invoice-2"}'
                WHERE id = 'trx-duplicate'
                """.formatted(SCHEMA));

        assertEquals(1, queryLong("""
                SELECT count(*)
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-duplicate'
                  AND transaction_revision = 4
                """.formatted(SCHEMA)));
        assertEquals(originalPayload, queryString("""
                SELECT payload::TEXT
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-duplicate'
                  AND transaction_revision = 4
                """.formatted(SCHEMA)));
        assertEquals(originalCreatedAt, queryOffsetDateTime("""
                SELECT created_at
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-duplicate'
                  AND transaction_revision = 4
                """.formatted(SCHEMA)));
    }

    @Test
    void shouldRejectOutboxUpdates() throws SQLException {
        insertTransaction("trx-update", 1);

        SQLException exception = assertThrows(SQLException.class, () -> execute("""
                UPDATE %s.transaction_outbox
                SET event_type = 'CHANGED'
                WHERE transaction_id = 'trx-update'
                """.formatted(SCHEMA)));

        assertEquals("55000", exception.getSQLState());
        assertTrue(exception.getMessage().contains("transaction_outbox rows are immutable"));
    }

    @Test
    void shouldKeepRowAndPayloadMetadataAligned() throws SQLException {
        insertTransaction("trx-metadata", 7);

        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id,
                            event_type,
                            transaction_revision,
                            schema_version,
                            occurred_at,
                            payload ->> 'eventId' AS payload_event_id,
                            payload ->> 'eventType' AS payload_event_type,
                            (payload ->> 'transactionRevision')::BIGINT AS payload_revision,
                            (payload ->> 'schemaVersion')::INTEGER AS payload_schema_version,
                            (payload ->> 'occurredAt')::TIMESTAMPTZ AS payload_occurred_at
                     FROM %s.transaction_outbox
                     WHERE transaction_id = 'trx-metadata'
                     """.formatted(SCHEMA))) {
            assertTrue(resultSet.next());
            assertEquals(Long.toString(resultSet.getLong("id")), resultSet.getString("payload_event_id"));
            assertEquals(resultSet.getString("event_type"), resultSet.getString("payload_event_type"));
            assertEquals(resultSet.getLong("transaction_revision"), resultSet.getLong("payload_revision"));
            assertEquals(resultSet.getInt("schema_version"), resultSet.getInt("payload_schema_version"));
            assertEquals(
                    resultSet.getObject("occurred_at", OffsetDateTime.class),
                    resultSet.getObject("payload_occurred_at", OffsetDateTime.class));
        }
    }

    @Test
    void shouldSerializeOccurredAtAsUtcRegardlessOfSessionTimezone() throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'Europe/Rome'");
            statement.execute("""
                    INSERT INTO %s.transaction (
                        id, "trxCode", "operationType", status, "trxDate", "userId",
                        "amountCents", "transactionRevision"
                    ) VALUES (
                        'trx-utc', 'code-utc', '00', 'CAPTURED', now(),
                        'user-utc', 100, 0
                    )
                    """.formatted(SCHEMA));

            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT occurred_at, payload ->> 'occurredAt' AS payload_occurred_at
                    FROM %s.transaction_outbox
                    WHERE transaction_id = 'trx-utc'
                    """.formatted(SCHEMA))) {
                assertTrue(resultSet.next());
                OffsetDateTime persistedOccurredAt =
                        resultSet.getObject("occurred_at", OffsetDateTime.class);
                OffsetDateTime payloadOccurredAt =
                        OffsetDateTime.parse(resultSet.getString("payload_occurred_at"));

                assertEquals(ZoneOffset.UTC, payloadOccurredAt.getOffset());
                assertEquals(persistedOccurredAt.toInstant(), payloadOccurredAt.toInstant());
            }
        }
    }

    @Test
    void shouldUpgradeLegacyCreatedAtAndRestoreEventInstants() throws SQLException {
        flyway("4").clean();
        flyway("4").migrate();

        execute("""
                DROP TRIGGER trg_transaction_outbox_reject_update
                    ON %s.transaction_outbox;
                ALTER TABLE %s.transaction_outbox
                    ALTER COLUMN created_at DROP DEFAULT,
                    ALTER COLUMN created_at TYPE TIMESTAMP
                        USING created_at AT TIME ZONE 'Europe/Rome',
                    ALTER COLUMN created_at SET DEFAULT now();
                INSERT INTO %s.transaction_outbox (
                    transaction_id, user_id, event_type, payload, created_at,
                    transaction_revision, schema_version, occurred_at
                ) VALUES
                    (
                        'trx-captured-legacy', 'user-1', 'TRANSACTION_CAPTURED',
                        '{"occurredAt":"2026-09-01T12:19:16.654+02:00"}',
                        '2026-09-01 12:19:16.654', 10, 1,
                        '2026-09-01T12:19:16.654+02:00'
                    ),
                    (
                        'trx-invoiced-legacy', 'user-1', 'TRANSACTION_INVOICED',
                        '{"occurredAt":"2026-09-01T10:19:17.286Z"}',
                        '2026-09-01 10:19:17.286', 11, 1,
                        '2026-09-01T10:19:17.286+02:00'
                    );
                CREATE TRIGGER trg_transaction_outbox_reject_update
                BEFORE UPDATE ON %s.transaction_outbox
                FOR EACH ROW
                EXECUTE FUNCTION %s.fn_reject_transaction_outbox_update()
                """.formatted(SCHEMA, SCHEMA, SCHEMA, SCHEMA, SCHEMA));

        flyway().migrate();

        assertEquals("timestamp with time zone", queryString("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'idpay-pagamenti'
                  AND table_name = 'transaction_outbox'
                  AND column_name = 'created_at'
                """));
        assertEventInstantRestored("trx-captured-legacy");
        assertEventInstantRestored("trx-invoiced-legacy");
    }

    @Test
    void shouldRollbackOutboxInsertWithTransaction() throws SQLException {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO %s.transaction (
                            id, "trxCode", "operationType", status, "trxDate", "userId",
                            "amountCents", "transactionRevision"
                        ) VALUES (
                            'trx-rollback', 'code-rollback', '00', 'INVOICED', now(),
                            'user-rollback', 100, 1
                        )
                        """.formatted(SCHEMA));
            }

            connection.rollback();
        }

        assertEquals(0, queryLong("""
                SELECT count(*)
                FROM %s.transaction_outbox
                WHERE transaction_id = 'trx-rollback'
                """.formatted(SCHEMA)));
    }

    private void insertTransaction(String transactionId, long revision) throws SQLException {
        execute("""
                INSERT INTO %s.transaction (
                    id, "trxCode", "operationType", status, "trxDate", "userId",
                    "amountCents", "transactionRevision"
                ) VALUES (
                    '%s', 'code-%s', '00', 'INVOICED', now(), 'user-%s', 100, %d
                )
                """.formatted(SCHEMA, transactionId, transactionId, transactionId, revision));
    }

    private void assertEventInstantRestored(String transactionId) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT created_at,
                            occurred_at,
                            (payload ->> 'occurredAt')::TIMESTAMPTZ AS payload_occurred_at
                     FROM %s.transaction_outbox
                     WHERE transaction_id = '%s'
                     """.formatted(SCHEMA, transactionId))) {
            assertTrue(resultSet.next());
            OffsetDateTime payloadOccurredAt =
                    resultSet.getObject("payload_occurred_at", OffsetDateTime.class);
            assertEquals(
                    payloadOccurredAt.toInstant(),
                    resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
            assertEquals(
                    payloadOccurredAt.toInstant(),
                    resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant());
        }
    }

    private Flyway flyway() {
        return flyway(null);
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .defaultSchema("idpay-pagamenti")
                .schemas("idpay-pagamenti")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private Connection connection() throws SQLException {
        return POSTGRES.createConnection("");
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }

    private String queryString(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private OffsetDateTime queryOffsetDateTime(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getObject(1, OffsetDateTime.class);
        }
    }
}
