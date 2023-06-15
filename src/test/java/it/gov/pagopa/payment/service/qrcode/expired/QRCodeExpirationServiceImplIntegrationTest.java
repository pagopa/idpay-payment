package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
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
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.qrCode.expirations.schedule.authorizationExpired=0 */1 * * * ?",
        "app.qrCode.expirations.schedule.cancelExpired=0 */1 * * * ?"
})
class QRCodeExpirationServiceImplIntegrationTest extends BaseIntegrationTest {

    private static final int N = 20;

    @Value("${app.qrCode.expirations.authorizationMinutes}")
    private int authorizationMinutes;
    @Value("${app.qrCode.expirations.cancelMinutes}")
    private int cancelMinutes;

    private OffsetDateTime OFFSET_NOW;
    private OffsetDateTime AUTHORIZATION_EXPIRED_CHARGE_DATE;
    private OffsetDateTime CANCEL_EXPIRED_CHARGE_DATE;

    @SpyBean
    private RewardCalculatorRestClient rewardCalculatorRestClientSpy;
    @SpyBean
    private TransactionNotifierService notifierServiceSpy;

    @Autowired
    private QRCodeAuthorizationExpiredService authorizationExpiredService;
    @Autowired
    private QRCodeCancelExpiredService cancelExpiredService;

    @Autowired
    private TransactionInProgressRepository repository;

    private QRCodeExpirationServiceImpl service;

    private List<TransactionInProgress> trxs = new ArrayList<>(N);
    private final Map<SyncTrxStatus, List<TransactionInProgress>> expiredTrxs = new HashMap<>();
    private final Map<SyncTrxStatus, List<TransactionInProgress>> validTrxs = new HashMap<>();;

    private void storeData() {
        trxs = repository.saveAll(
                IntStream.range(0, N).mapToObj(i -> buildValidTransactionInProgress(i, i < (N / 2))).toList()
        );
    }

    private TransactionInProgress buildValidTransactionInProgress(int bias, boolean expired) {
        SyncTrxStatus status = getStatus(bias);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(bias, status)
                .elaborationDateTime(LocalDateTime.now().minusMinutes(50))
                .trxChargeDate(expired ? getExpiredChargeDate(bias) : OFFSET_NOW)
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
        return switch (bias % 3) {
            case 1 -> SyncTrxStatus.CREATED;
            case 2 -> SyncTrxStatus.IDENTIFIED;
            default -> SyncTrxStatus.AUTHORIZED;
        };
    }

    private OffsetDateTime getExpiredChargeDate(int bias) {
        return switch (bias % 3) {
            case 1, 2 -> AUTHORIZATION_EXPIRED_CHARGE_DATE;
            default -> CANCEL_EXPIRED_CHARGE_DATE;
        };
    }

    @BeforeEach
    void init() {
        OFFSET_NOW = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        AUTHORIZATION_EXPIRED_CHARGE_DATE = OFFSET_NOW.minusMinutes(authorizationMinutes+50L);
        CANCEL_EXPIRED_CHARGE_DATE = OFFSET_NOW.minusMinutes(cancelMinutes+50L);

        service = new QRCodeExpirationServiceImpl(authorizationExpiredService, cancelExpiredService);
        storeData();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAllById(trxs.stream().map(TransactionInProgress::getId).toList());
    }

    @Test
    void test() {
        // waitFor expired trxs deleted from db
        long[] count = {0};
        TestUtils.waitFor(() -> (count[0]=repository.count()) <= extractIdsFromTrxsMap(validTrxs).size(),
                () -> "Expected %d trxs, found %d".formatted(N/2, count[0]),
                200,
                1000);

        // valid trxs still on db
        List<TransactionInProgress> result2 = repository.findAllById(extractIdsFromTrxsMap(validTrxs));
        List<TransactionInProgress> expected = validTrxs.values().stream().flatMap(List::stream).toList();
        Assertions.assertEquals(sortAndCleanDates(expected), sortAndCleanDates(result2));

//        // verify call to rewardCalculator cancel for IDENTIFIED expired trxs
//        expiredTrxs.get(SyncTrxStatus.IDENTIFIED).forEach(t -> Mockito.verify(rewardCalculatorRestClientSpy).cancelTransaction(t.getId()));
//
//        // verify AUTHORIZED expired trxs to be notified in idpay-transaction queue
//        expiredTrxs.get(SyncTrxStatus.AUTHORIZED).forEach(t -> Mockito.verify(notifierServiceSpy).notify());
    }

    private List<String> extractIdsFromTrxsMap(Map<SyncTrxStatus, List<TransactionInProgress>> trxsMap) {
        return trxsMap.values().stream().flatMap(List::stream).map(TransactionInProgress::getId).toList();
    }

    private List<TransactionInProgress> sortAndCleanDates(List<TransactionInProgress> trxsList) {
        trxsList.forEach(t -> t.setElaborationDateTime(null));
        return trxsList.stream().sorted(Comparator.comparing(TransactionInProgress::getId)).toList();
    }

}