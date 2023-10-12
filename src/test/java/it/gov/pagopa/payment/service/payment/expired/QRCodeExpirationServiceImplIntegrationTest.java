package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.connector.event.trx.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = {
        "app.qrCode.expirations.schedule.authorizationExpired=0/30 * * * * ?",
        "app.qrCode.expirations.schedule.cancelExpired=0/30 * * * * ?"
})
class QRCodeExpirationServiceImplIntegrationTest extends BaseIntegrationTest {

    private static final int N = 20;
    private static final int N_EXPIRED = N/2;

    @Value("${app.qrCode.expirations.authorizationMinutes}")
    private int authorizationMinutes;
    @Value("${app.qrCode.expirations.cancelMinutes}")
    private int cancelMinutes;

    private OffsetDateTime OFFSET_NOW;
    private OffsetDateTime AUTHORIZATION_EXPIRED_DATE;
    private OffsetDateTime CANCEL_EXPIRED_DATE;

    @SpyBean
    private RewardCalculatorRestClient rewardCalculatorRestClientSpy;

    @Autowired
    private TransactionInProgress2TransactionOutcomeDTOMapper transactionOutcomeDTOMapper;

    @Autowired
    private TransactionInProgressRepository repository;

    private final List<TransactionInProgress> trxs = new ArrayList<>(N);
    private final Map<SyncTrxStatus, List<TransactionInProgress>> expiredTrxs = new HashMap<>();
    private final Map<SyncTrxStatus, List<TransactionInProgress>> validTrxs = new HashMap<>();

    private void storeData() {
        trxs.addAll(repository.saveAll(
                IntStream.range(0, N).mapToObj(i -> buildValidTransactionInProgress(i, i < N_EXPIRED)).toList()
        ));
    }

    private TransactionInProgress buildValidTransactionInProgress(int bias, boolean expired) {
        SyncTrxStatus status = getStatus(bias);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(bias, status)
                .elaborationDateTime(LocalDateTime.now().minusSeconds(1))
                .trxDate(expired ? getExpiredDate(bias) : OFFSET_NOW)
                .build();

        if (SyncTrxStatus.IDENTIFIED.equals(status) && bias%2==1) {
            trx.setId("IDENTIFIEDTRX_REWARDKO_%d".formatted(bias));
        }

        if (expired) {
            expiredTrxs.computeIfAbsent(trx.getStatus(), k -> new ArrayList<>()).add(trx);
        } else {
            validTrxs.computeIfAbsent(trx.getStatus(), k -> new ArrayList<>()).add(trx);
        }

        return trx;
    }

    private SyncTrxStatus getStatus(int bias) {
        return switch (bias % 4) {
            case 1 -> SyncTrxStatus.CREATED;
            case 2 -> SyncTrxStatus.IDENTIFIED;
            case 3 -> SyncTrxStatus.REJECTED;
            default -> SyncTrxStatus.AUTHORIZED;
        };
    }

    private OffsetDateTime getExpiredDate(int bias) {
        return switch (bias % 4) {
            case 1, 2, 3 -> AUTHORIZATION_EXPIRED_DATE;
            default -> CANCEL_EXPIRED_DATE;
        };
    }

    @BeforeEach
    void init() {
        OFFSET_NOW = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        AUTHORIZATION_EXPIRED_DATE = OFFSET_NOW.minusMinutes(authorizationMinutes+50L);
        CANCEL_EXPIRED_DATE = OFFSET_NOW.minusMinutes(cancelMinutes+50L);

        storeData();
    }

    @AfterEach
    void verifyNotMoreInvocations() {
        Mockito.verifyNoMoreInteractions(rewardCalculatorRestClientSpy);
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAllById(trxs.stream().map(TransactionInProgress::getId).toList());
    }

    @Test
    void test() {
        // waitFor expired trxs deleted from db
        long[] count = {0};
        List<String> testIds = trxs.stream().map(TransactionInProgress::getId).toList();
        TestUtils.waitFor(() -> (count[0]=repository.findAllById(testIds).size()) == N_EXPIRED,
                () -> "Expected %d trxs, found %d".formatted(N_EXPIRED, count[0]),
                60,
                1000);

        // valid trxs still on db
        checkNotExpiredTrxs();

        // verify call to rewardCalculator cancel for IDENTIFIED and REJECTED expired trxs
        expiredTrxs.get(SyncTrxStatus.IDENTIFIED).forEach(t -> Mockito.verify(rewardCalculatorRestClientSpy).cancelTransaction(t.getId()));

        // verify AUTHORIZED expired trxs to be notified in idpay-transaction queue
        checkConfirmEvents();
    }

    private void checkNotExpiredTrxs() {
        List<TransactionInProgress> expected = validTrxs.values().stream().flatMap(List::stream).toList();
        List<TransactionInProgress> result = repository.findAllById(extractIdsFromTrxsMap(validTrxs));
        Assertions.assertEquals(sortAndCleanDates(expected), sortAndCleanDates(result));
    }

    private List<String> extractIdsFromTrxsMap(Map<SyncTrxStatus, List<TransactionInProgress>> trxsMap) {
        return trxsMap.values().stream().flatMap(List::stream).map(TransactionInProgress::getId).toList();
    }

    private List<TransactionInProgress> sortAndCleanDates(Collection<TransactionInProgress> trxs) {
        trxs.forEach(t -> {
            Assertions.assertNotNull(t.getElaborationDateTime());
            t.setElaborationDateTime(null);
        });
        return trxs.stream().sorted(Comparator.comparing(TransactionInProgress::getId)).toList();
    }

    private void checkConfirmEvents() {
        Map<String, TransactionInProgress> expectedEvents = expiredTrxs.get(SyncTrxStatus.AUTHORIZED).stream().map(this::trxInProgress2TrxOutcome).collect(Collectors.toMap(TransactionInProgress::getId, Function.identity()));

        List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtilitiesService.consumeMessages(topicConfirmNotification, expectedEvents.size(), 15000);

        Set<TransactionInProgress> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionOutcomeDTO out = TestUtils.jsonDeserializer(r.value(), TransactionOutcomeDTO.class);
                    assertEquals(SyncTrxStatus.REWARDED, out.getStatus());
                    assertEquals(out.getMerchantId(), r.key());

                    assertTrue(out.getElaborationDateTime().isAfter(expectedEvents.get(out.getId()).getElaborationDateTime()));

                    return out;
                })
                .collect(Collectors.toSet());

        Assertions.assertEquals(
                sortAndCleanDates(expectedEvents.values()),
                sortAndCleanDates(eventsResult)
        );
    }

    private TransactionOutcomeDTO trxInProgress2TrxOutcome(TransactionInProgress t) {
        t.setStatus(SyncTrxStatus.REWARDED);
        return transactionOutcomeDTOMapper.apply(t);
    }

}