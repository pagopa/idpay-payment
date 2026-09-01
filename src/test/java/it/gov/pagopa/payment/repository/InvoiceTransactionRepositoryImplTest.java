package it.gov.pagopa.payment.repository;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.payment.connector.event.trx.RewardTransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionEventType;
import it.gov.pagopa.payment.exception.custom.TransactionConflictException;
import it.gov.pagopa.payment.model.InvoiceData;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InvoiceTransactionRepositoryImplTest.TestConfig.class)
class InvoiceTransactionRepositoryImplTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @jakarta.annotation.Resource
    private InvoiceTransactionRepository invoiceTransactionRepository;

    @jakarta.annotation.Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM \"idpay-pagamenti\".transaction_outbox");
        jdbcTemplate.update("DELETE FROM \"idpay-pagamenti\".transaction");
    }

    @Test
    void shouldCreateTypedInitialAndReplacementEvents() {
        insertTransaction("trx-invoice", SyncTrxStatus.CAPTURED, 0, "user-1");

        Transaction invoiced = invoiceTransactionRepository.updateInvoiceAndCreateEvent(command(
                "trx-invoice", SyncTrxStatus.CAPTURED, 0,
                TransactionEventType.TRANSACTION_INVOICED, "invoice-1.pdf"));
        Transaction replaced = invoiceTransactionRepository.updateInvoiceAndCreateEvent(command(
                "trx-invoice", SyncTrxStatus.INVOICED, 1,
                TransactionEventType.TRANSACTION_INVOICE_REPLACED, "invoice-2.pdf"));

        assertEquals(1L, invoiced.getTransactionRevision());
        assertEquals(2L, replaced.getTransactionRevision());
        assertEquals(1, eventCount("trx-invoice", 1, "TRANSACTION_INVOICED"));
        assertEquals(1, eventCount("trx-invoice", 2, "TRANSACTION_INVOICE_REPLACED"));
        assertEquals("00", jdbcTemplate.queryForObject("""
                SELECT payload ->> 'operationType'
                FROM "idpay-pagamenti".transaction_outbox
                WHERE transaction_id = 'trx-invoice'
                  AND transaction_revision = 2
                """, String.class));
        assertEquals("invoice-2.pdf", jdbcTemplate.queryForObject("""
                SELECT payload -> 'invoiceData' ->> 'filename'
                FROM "idpay-pagamenti".transaction_outbox
                WHERE transaction_id = 'trx-invoice'
                  AND transaction_revision = 2
                """, String.class));
    }

    @Test
    void shouldReplaceRewardedTransactionAsInvoiced() {
        insertTransaction("trx-rewarded", SyncTrxStatus.REWARDED, 8, "user-1");

        Transaction updated = invoiceTransactionRepository.updateInvoiceAndCreateEvent(command(
                "trx-rewarded", SyncTrxStatus.REWARDED, 8,
                TransactionEventType.TRANSACTION_INVOICE_REPLACED, "invoice-9.pdf"));

        assertEquals(SyncTrxStatus.INVOICED, updated.getStatus());
        assertEquals(9L, updated.getTransactionRevision());
        assertEquals(1, eventCount("trx-rewarded", 9, "TRANSACTION_INVOICE_REPLACED"));
    }

    @Test
    void shouldRejectStaleAndConcurrentRevisions() throws Exception {
        insertTransaction("trx-concurrent", SyncTrxStatus.INVOICED, 5, "user-1");
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> executeReplacement(start, "invoice-a.pdf"));
            Future<Boolean> second = executor.submit(() -> executeReplacement(start, "invoice-b.pdf"));
            start.countDown();

            assertEquals(List.of(false, true), List.of(first.get(), second.get()).stream().sorted().toList());
        }

        assertEquals(1, eventCount("trx-concurrent", 6, "TRANSACTION_INVOICE_REPLACED"));
        assertEquals(6L, jdbcTemplate.queryForObject("""
                SELECT "transactionRevision"
                FROM "idpay-pagamenti".transaction
                WHERE id = 'trx-concurrent'
                """, Long.class));
    }

    @Test
    void shouldRollbackTransactionUpdateWhenOutboxInsertFails() {
        insertTransaction("trx-rollback", SyncTrxStatus.INVOICED, 2, "user-1");
        jdbcTemplate.update("""
                UPDATE "idpay-pagamenti".transaction
                SET "userId" = NULL
                WHERE id = 'trx-rollback'
                """);
        jdbcTemplate.update("""
                DELETE FROM "idpay-pagamenti".transaction_outbox
                WHERE transaction_id = 'trx-rollback'
                """);

        assertThrows(ConstraintViolationException.class, () ->
                invoiceTransactionRepository.updateInvoiceAndCreateEvent(command(
                        "trx-rollback", SyncTrxStatus.INVOICED, 2,
                        TransactionEventType.TRANSACTION_INVOICE_REPLACED, "invoice-3.pdf")));

        assertEquals(2L, jdbcTemplate.queryForObject("""
                SELECT "transactionRevision"
                FROM "idpay-pagamenti".transaction
                WHERE id = 'trx-rollback'
                """, Long.class));
        assertEquals(0, eventCount("trx-rollback", 3, "TRANSACTION_INVOICE_REPLACED"));
    }

    private boolean executeReplacement(CountDownLatch start, String filename) throws InterruptedException {
        start.await();
        try {
            invoiceTransactionRepository.updateInvoiceAndCreateEvent(command(
                    "trx-concurrent", SyncTrxStatus.INVOICED, 5,
                    TransactionEventType.TRANSACTION_INVOICE_REPLACED, filename));
            return true;
        } catch (TransactionConflictException e) {
            return false;
        }
    }

    private InvoiceTransactionCommand command(
            String transactionId,
            SyncTrxStatus expectedStatus,
            long expectedRevision,
            TransactionEventType eventType,
            String filename) {
        return new InvoiceTransactionCommand(
                transactionId,
                "initiative-1",
                "merchant-1",
                expectedStatus,
                expectedRevision,
                InvoiceData.builder().filename(filename).docNumber("DOC").build(),
                LocalDateTime.now(),
                "franchise",
                "PHYSICAL",
                "business",
                "fiscal-code",
                eventType);
    }

    private void insertTransaction(
            String transactionId,
            SyncTrxStatus status,
            long revision,
            String userId) {
        jdbcTemplate.update("""
                INSERT INTO "idpay-pagamenti".transaction (
                    id, "trxCode", "operationType", status, "trxDate", "userId",
                    "merchantId", "pointOfSaleId", "initiativeId", initiatives,
                    "amountCents", "transactionRevision", "invoiceData",
                    "additionalProperties"
                ) VALUES (?, ?, '00', ?, now(), ?, 'merchant-1', 'pos-1',
                    'initiative-1', '["initiative-1"]', 100, ?, ?::jsonb,
                    '{"productName":"product-1"}')
                """,
                transactionId,
                "code-" + transactionId,
                status.name(),
                userId,
                revision,
                status == SyncTrxStatus.CAPTURED
                        ? null
                        : "{\"filename\":\"old.pdf\",\"docNumber\":\"OLD\"}");
    }

    private int eventCount(String transactionId, long revision, String eventType) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM "idpay-pagamenti".transaction_outbox
                WHERE transaction_id = ?
                  AND transaction_revision = ?
                  AND event_type = ?
                """, Integer.class, transactionId, revision, eventType);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = TransactionRepository.class)
    @Import({InvoiceTransactionRepositoryImpl.class, RewardTransactionMapper.class, JsonConfig.class})
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setURL(POSTGRES.getJdbcUrl());
            dataSource.setUser(POSTGRES.getUsername());
            dataSource.setPassword(POSTGRES.getPassword());
            return dataSource;
        }

        @Bean(initMethod = "migrate")
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .defaultSchema("idpay-pagamenti")
                    .schemas("idpay-pagamenti")
                    .load();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource dataSource,
                Flyway flyway) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(Transaction.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "none",
                    "hibernate.default_schema", "idpay-pagamenti",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
