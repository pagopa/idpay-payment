package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@MongoTest
@Slf4j
class TransactionInProgressRepositoryExtImplTest {

  private static final String INITIATIVE_ID = "INITIATIVEID1";
  private static final String MERCHANT_ID = "MERCHANTID1";
  private static final String POINT_OF_SALE_ID = "POINTOFSALEID1";
  private static final String USER_ID = "USERID1";
  private static final String PRODUCT_GTIN = "PRODUCTGTIN1";
  public static final int EXPIRATION_MINUTES = 4350;
  public static final int EXPIRATION_MINUTES_IDPAY_CODE = 5;
  private static final String TRX_ID = "TRX_ID";

  @MockitoBean
    private AppConfigurationProperties.ExtendedTransactions extendedTransactions;
    @Autowired
  protected TransactionInProgressRepository transactionInProgressRepository;
  @Autowired
  protected MongoTemplate mongoTemplate;

    @BeforeEach
    void initParams() {
        when(extendedTransactions.getSendExpiredSendBatchSize()).thenReturn(1);
        when(extendedTransactions.getUpdateBatchSize()).thenReturn(1);
        when(extendedTransactions.getStaleMinutesThreshold()).thenReturn(10);
    }


  @AfterEach
  void clearTestData() {
    mongoTemplate.findAllAndRemove(
        new Query(
            Criteria.where(TransactionInProgress.Fields.id)
                .regex("^MOCKEDTRANSACTION_qr-code_[0-9]+$")),
        TransactionInProgress.class);
  }

  @Test
  void createIfExists() {

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    TransactionInProgress transactionInProgressSecond =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
    transactionInProgressSecond.setTrxCode(transactionInProgress.getTrxCode());

    UpdateResult updatedFirst =
        transactionInProgressRepository.createIfExists(
            transactionInProgress, transactionInProgress.getTrxCode());
    assertNotNull(updatedFirst.getUpsertedId());
    assertEquals(0L, updatedFirst.getMatchedCount());

    UpdateResult updatedSecond =
        transactionInProgressRepository.createIfExists(
            transactionInProgressSecond, transactionInProgress.getTrxCode());
    assertNull(updatedSecond.getUpsertedId());
    assertEquals(1L, updatedSecond.getMatchedCount());

    TransactionInProgress result =
        mongoTemplate.findById(transactionInProgress.getId(), TransactionInProgress.class);
    assertNotNull(result);
    TestUtils.checkNotNullFields(
        result,
        "userId",
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");
  }

  @Test
  void findByTrxCodeThrottled() {
    TransactionInProgress notFoundResult = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(
        "DUMMYID", EXPIRATION_MINUTES);
    assertNull(notFoundResult);

    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transactionInProgressRepository.save(transaction);

    TransactionInProgress result = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(
        transaction.getTrxCode(), EXPIRATION_MINUTES);

    assertNotNull(result);
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    assertTrue(
        result.getTrxChargeDate().isAfter(OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES)));
    TestUtils.checkNotNullFields(
        result, "userId", "elaborationDateTime", "reward", "rejectionReasons", "rewards",
        "authDate", "trxChargeDate", "initiativeRejectionReasons", "initiativeEndDate",
        "voucherAmountCents", "invoiceData", "creditNoteData", "franchiseName", "pointOfSaleType", "familyId");

    TooManyRequestsException exception =
        assertThrows(
            TooManyRequestsException.class,
            () -> transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(
                "trxcode1", EXPIRATION_MINUTES));

    assertEquals(ExceptionCode.TOO_MANY_REQUESTS, exception.getCode());
    assertEquals("Too many requests on trx having trCode: trxcode1", exception.getMessage());
  }


  @Test
  void findByTrxCodeThrottled_Concurrent() {
    int n = 10;
    TransactionInProgress stored =
        transactionInProgressRepository.save(
            TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.IDENTIFIED));

    Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> mongoCommandsByType = executeConcurrentLocks(
        n,
        () -> {
          try {
            transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(
                stored.getTrxCode(), EXPIRATION_MINUTES);
            return true;
          } catch (TooManyRequestsException e) {
            assertEquals(ExceptionCode.TOO_MANY_REQUESTS, e.getCode());
            assertEquals("Too many requests on trx having trCode: trxcode0", e.getMessage());
            return false;
          }
        }
    );
    Assertions.assertEquals(n - 1, mongoCommandsByType.get("aggregate").get(0).getValue());
  }

  @Test
  void updateTrxAuthorized() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
    transaction.setUserId("USERID%d".formatted(1));
    transactionInProgressRepository.save(transaction);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);

    transactionInProgressRepository.updateTrxAuthorized(transaction, authPaymentDTO,
        CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(),
            List.of()));
    TransactionInProgress result =
        transactionInProgressRepository.findById(transaction.getId()).orElse(null);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(
        result,
        "authDate",
        "elaborationDateTime",
        "reward",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");
    Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

  }

  @Test
  void updateTrxAuthorized_barCode() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
    transaction.setUserId("USERID%d".formatted(1));
    transaction.setChannel(RewardConstants.TRX_CHANNEL_BARCODE);
    transactionInProgressRepository.save(transaction);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);

    transactionInProgressRepository.updateTrxAuthorized(transaction, authPaymentDTO,
        CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(),
            List.of()));
    TransactionInProgress result =
        transactionInProgressRepository.findById(transaction.getId()).orElse(null);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(
        result,
        "authDate",
        "elaborationDateTime",
        "reward",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");
    Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

  }

  @Test
  void findByTrxCode() {

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(
        "trxcode1");
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "userId",
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");

    transactionInProgress.setTrxDate(OffsetDateTime.now().minusDays(30));
    transactionInProgress.setTrxEndDate(OffsetDateTime.now().minusDays(11));
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    Assertions.assertNull(resultSecondSave);
  }

  @Test
  void findByTrxId() {

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findByTrxIdAndAuthorizationNotExpired(
        transactionInProgress.getId(), EXPIRATION_MINUTES_IDPAY_CODE);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "userId",
        "authDate",
        "elaborationDateTime",
        "reward",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");

    transactionInProgress.setTrxDate(
        OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES_IDPAY_CODE));
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findByTrxIdAndAuthorizationNotExpired(
            transactionInProgress.getId(), EXPIRATION_MINUTES_IDPAY_CODE);
    Assertions.assertNull(resultSecondSave);
  }

  @Test
  void updateTrxRelateUserIdentified() {
    // Given
    TransactionInProgress trx =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(trx);

    transactionInProgressRepository.updateTrxRelateUserIdentified(trx.getId(),
        USER_ID, "IDPAYCODE");

    // When
    TransactionInProgress resultUpdate =
        transactionInProgressRepository.findById(trx.getId()).orElse(null);

    // Then
    Assertions.assertNotNull(resultUpdate);
    TestUtils.checkNotNullFields(resultUpdate,
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");
    Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultUpdate.getStatus());
    Assertions.assertEquals(USER_ID, resultUpdate.getUserId());
  }

  @Test
  void updateTrxWithStatus_Identified() {
    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "userId",
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");

    AuthPaymentDTO preview = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
    preview.setRewardCents(500L);
    preview.setRejectionReasons(List.of("REASON"));
    preview.setRewards(Map.of("ID", new Reward()));

    transactionInProgress.setUserId("USERID1");
    preview.setCounterVersion(10L);
    transactionInProgress.setTrxChargeDate(OffsetDateTime.now());
    transactionInProgress.setAmountCents(10L);

    transactionInProgressRepository.updateTrxWithStatusForPreview(transactionInProgress, preview,
        Map.of(transactionInProgress.getInitiativeId(), List.of("REASON")), "CHANNEL",
        SyncTrxStatus.IDENTIFIED);

    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(
        resultSecondSave, "authDate", "elaborationDateTime", "trxChargeDate", "initiativeEndDate",
        "voucherAmountCents", "invoiceData", "creditNoteData", "franchiseName", "pointOfSaleType", "familyId");
    Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
  }

  @Test
  void updateTrxWithStatusForPreview_Identified() {

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "userId",
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");

    transactionInProgress.setStatus(SyncTrxStatus.IDENTIFIED);
    transactionInProgress.setUserId("USERID1");
    transactionInProgress.setRewardCents(500L);
    transactionInProgress.setRejectionReasons(List.of("REASON"));
    transactionInProgress.setInitiativeRejectionReasons(
        Map.of(transactionInProgress.getInitiativeId(), List.of("REASON")));
    transactionInProgress.setRewards(Map.of("ID", new Reward()));
    transactionInProgress.setChannel("CHANNEL");
    transactionInProgress.setCounterVersion(10L);
    transactionInProgress.setTrxChargeDate(OffsetDateTime.now());
    transactionInProgress.setAmountCents(10L);

    transactionInProgressRepository.updateTrxWithStatus(transactionInProgress);

    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(
        resultSecondSave, "authDate", "elaborationDateTime", "trxChargeDate", "initiativeEndDate",
        "voucherAmountCents", "invoiceData", "creditNoteData", "franchiseName", "pointOfSaleType", "familyId");
    Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
  }

  @Test
  void updateTrxRejected() {

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "userId",
        "authDate",
        "elaborationDateTime",
        "rewardCents",
        "rejectionReasons",
        "rewards",
        "trxChargeDate",
        "initiativeRejectionReasons",
        "initiativeEndDate",
        "voucherAmountCents",
        "invoiceData",
        "creditNoteData",
        "franchiseName",
        "pointOfSaleType",
        "familyId");

    transactionInProgressRepository.updateTrxRejected(
        "MOCKEDTRANSACTION_qr-code_1", "USERID1", List.of("REJECTIONREASON1"),
        Map.of(transactionInProgress.getInitiativeId(), List.of("REJECTIONREASON1")), "CHANNEL");
    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave, "authDate", "elaborationDateTime", "reward",
        "rewards", "trxChargeDate", "initiativeEndDate", "voucherAmountCents", "invoiceData", "creditNoteData",
        "franchiseName", "pointOfSaleType", "familyId");
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
  }

  @Test
  void findByFilter() {
    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transactionInProgress.setUserId(USER_ID);
    transactionInProgressRepository.save(transactionInProgress);

    Criteria criteria = transactionInProgressRepository.getCriteria(
        MERCHANT_ID, POINT_OF_SALE_ID, INITIATIVE_ID,
        USER_ID, SyncTrxStatus.IDENTIFIED.toString(), null);
    Pageable paging = PageRequest.of(0, 10);

    List<TransactionInProgress> transactionInProgressList =
        transactionInProgressRepository.findByFilter(criteria, paging);

    assertFalse(transactionInProgressList.isEmpty());

    TransactionInProgress savedTrx = transactionInProgressList.get(0);

    assertEquals(transactionInProgress.getTrxCode(), savedTrx.getTrxCode());
    assertEquals(transactionInProgress.getTrxDate(), savedTrx.getTrxDate());
    assertEquals(transactionInProgress.getIdTrxAcquirer(), savedTrx.getIdTrxAcquirer());
    assertEquals(transactionInProgress.getOperationType(), savedTrx.getOperationType());
    assertEquals(transactionInProgress.getUserId(), savedTrx.getUserId());
  }


  @Test
  void getCount() {
    TransactionInProgress transactionInProgress1 =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    TransactionInProgress transactionInProgress2 =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
    transactionInProgress2.setInitiativeId(INITIATIVE_ID);
    transactionInProgress2.setInitiatives(List.of(INITIATIVE_ID));
    transactionInProgress2.setMerchantId(MERCHANT_ID);
    transactionInProgress2.setPointOfSaleId(POINT_OF_SALE_ID);
    TransactionInProgress transactionInProgress3 =
        TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.AUTHORIZED);
    transactionInProgress3.setInitiativeId(INITIATIVE_ID);
    transactionInProgress3.setInitiatives(List.of(INITIATIVE_ID));
    transactionInProgress3.setMerchantId(MERCHANT_ID);
    transactionInProgress3.setPointOfSaleId(POINT_OF_SALE_ID);
    transactionInProgressRepository.save(transactionInProgress1);
    transactionInProgressRepository.save(transactionInProgress2);
    transactionInProgressRepository.save(transactionInProgress3);
    Criteria criteria = transactionInProgressRepository.getCriteria(MERCHANT_ID, POINT_OF_SALE_ID,
        INITIATIVE_ID, null, null, null);
    long count = transactionInProgressRepository.getCount(criteria);
    assertEquals(3, count);
  }

  @Test
  void findPageByFilter() {
    Map<String, String> additionalProperties = new HashMap<>();
    additionalProperties.put("productGtin", PRODUCT_GTIN);

    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setMerchantId(MERCHANT_ID);
    trx.setPointOfSaleId(POINT_OF_SALE_ID);
    trx.setInitiativeId(INITIATIVE_ID);
    trx.setUserId(USER_ID);
    trx.setStatus(SyncTrxStatus.AUTHORIZED);
    trx.setAdditionalProperties(additionalProperties);

    transactionInProgressRepository.save(trx);

    Pageable pageable = PageRequest.of(0, 10);

    Page<TransactionInProgress> result = transactionInProgressRepository.findPageByFilter(
        MERCHANT_ID,
        POINT_OF_SALE_ID,
        INITIATIVE_ID,
        USER_ID,
        SyncTrxStatus.AUTHORIZED.toString(),
        PRODUCT_GTIN,
        pageable
    );

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(trx.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findPageByFilterWithAggregations() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setMerchantId(MERCHANT_ID);
    trx.setPointOfSaleId(POINT_OF_SALE_ID);
    trx.setInitiativeId(INITIATIVE_ID);
    trx.setUserId(USER_ID);
    trx.setStatus(SyncTrxStatus.AUTHORIZED);

    transactionInProgressRepository.save(trx);

    Pageable pageableStatus = PageRequest.of(0, 10, Sort.by("status"));
    Page<TransactionInProgress> resultStatus = transactionInProgressRepository.findPageByFilter(
        MERCHANT_ID,
        POINT_OF_SALE_ID,
        INITIATIVE_ID,
        USER_ID,
        SyncTrxStatus.AUTHORIZED.toString(),
        null,
        pageableStatus
    );
    assertNotNull(resultStatus);
    assertEquals(1, resultStatus.getTotalElements());
    assertEquals(trx.getId(), resultStatus.getContent().get(0).getId());
  }

  @Test
  void findPageByFilterSortedByProductName() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setMerchantId(MERCHANT_ID);
    trx.setPointOfSaleId(POINT_OF_SALE_ID);
    trx.setInitiativeId(INITIATIVE_ID);
    trx.setUserId(USER_ID);
    trx.setStatus(SyncTrxStatus.AUTHORIZED);

    transactionInProgressRepository.save(trx);

    Pageable pageable = PageRequest.of(0, 10, Sort.by("productName"));

    Page<TransactionInProgress> result = transactionInProgressRepository.findPageByFilter(
        MERCHANT_ID,
        POINT_OF_SALE_ID,
        INITIATIVE_ID,
        USER_ID,
        SyncTrxStatus.AUTHORIZED.toString(),
        null,
        pageable
    );

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(trx.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findPageByFilterSortedByOtherField() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
    trx.setMerchantId(MERCHANT_ID);
    trx.setPointOfSaleId(POINT_OF_SALE_ID);
    trx.setInitiativeId(INITIATIVE_ID);
    trx.setUserId(USER_ID);
    trx.setStatus(SyncTrxStatus.AUTHORIZED);
    transactionInProgressRepository.save(trx);

    Pageable pageable = PageRequest.of(0, 10, Sort.by("trxDate"));

    Page<TransactionInProgress> result = transactionInProgressRepository.findPageByFilter(
        MERCHANT_ID,
        POINT_OF_SALE_ID,
        INITIATIVE_ID,
        USER_ID,
        SyncTrxStatus.AUTHORIZED.toString(),
        null,
        pageable
    );

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }


  @Test
  void getCriteria() {
    Criteria criteria = transactionInProgressRepository.getCriteria(MERCHANT_ID, null,
        INITIATIVE_ID, USER_ID, SyncTrxStatus.AUTHORIZED.toString(), null);
    assertEquals(4, criteria.getCriteriaObject().size());
  }

  @Test
  void getCriteria1() {
    Criteria criteria1 = transactionInProgressRepository.getCriteria(MERCHANT_ID, POINT_OF_SALE_ID,
        INITIATIVE_ID, USER_ID, SyncTrxStatus.AUTHORIZED.toString(), null);
    assertEquals(5, criteria1.getCriteriaObject().size());
  }

  @Test
  void findAuthorizationExpiredTransaction() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transactionInProgressRepository.save(transaction);

    TransactionInProgress notExpiredTrxResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNull(notExpiredTrxResult);

    TransactionInProgress transactionExpired =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
    transactionExpired.setTrxDate(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES).minusSeconds(1));
    transactionInProgressRepository.save(transactionExpired);

    TransactionInProgress expiredTrxResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNotNull(expiredTrxResult);

    Assertions.assertEquals(transactionExpired.getTrxCode(), expiredTrxResult.getTrxCode());
    Assertions.assertEquals(transactionExpired.getTrxDate().toInstant(), expiredTrxResult.getTrxDate().toInstant());
    Assertions.assertEquals(transactionExpired.getIdTrxAcquirer(), expiredTrxResult.getIdTrxAcquirer());
    Assertions.assertEquals(transactionExpired.getOperationType(), expiredTrxResult.getOperationType());

    Assertions.assertNull(
        transactionInProgressRepository.findAuthorizationExpiredTransaction("DUMMYINITIATIVEID",
            EXPIRATION_MINUTES));

    TransactionInProgress expiredTrxThrottledResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNull(expiredTrxThrottledResult);
  }


  @Test
  void findAuthorizationExpiredTransaction_whenExtendedAuthorizationTrue_thenNotReturnedAndStillPresentInDb() {
    TransactionInProgress trxExpiredExtendedAuth = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trxExpiredExtendedAuth.setExtendedAuthorization(true);
    trxExpiredExtendedAuth.setTrxDate(OffsetDateTime.now()
        .truncatedTo(ChronoUnit.MILLIS)
        .minusMinutes(EXPIRATION_MINUTES));
    transactionInProgressRepository.save(trxExpiredExtendedAuth);

    TransactionInProgress trxExpired = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
    trxExpired.setExtendedAuthorization(false);
    trxExpired.setTrxDate(OffsetDateTime.now()
        .truncatedTo(ChronoUnit.MILLIS)
        .minusMinutes(EXPIRATION_MINUTES));
    transactionInProgressRepository.save(trxExpired);

    TransactionInProgress expiredTrxResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES);

    Assertions.assertNotNull(expiredTrxResult);
    Assertions.assertNotEquals(Boolean.TRUE, expiredTrxResult.getExtendedAuthorization());

    TransactionInProgress secondResult =
        transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES);
    Assertions.assertNull(secondResult);

    TransactionInProgress trxFromDb = mongoTemplate.findById(trxExpiredExtendedAuth.getId(), TransactionInProgress.class);
    Assertions.assertNotNull(trxFromDb);
    assertEquals(Boolean.TRUE, trxFromDb.getExtendedAuthorization());
  }

  @Test
  void testFindAuthorizationExpiredTransaction_concurrent() {
    TransactionInProgress transactionExpired =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
    transactionExpired.setTrxDate(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
    transactionInProgressRepository.save(transactionExpired);

    executeConcurrentLocks(10, () ->
        transactionInProgressRepository.findAuthorizationExpiredTransaction(null,
            EXPIRATION_MINUTES) != null);
  }

  @Test
  void findCancelExpiredTransaction() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    transactionInProgressRepository.save(transaction);

    TransactionInProgress notExpiredTrxResult = transactionInProgressRepository.findCancelExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNull(notExpiredTrxResult);

    TransactionInProgress transactionExpired =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
    transactionExpired.setTrxDate(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
    transactionInProgressRepository.save(transactionExpired);

    TransactionInProgress expiredTrxResult = transactionInProgressRepository.findCancelExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNotNull(expiredTrxResult);

    Assertions.assertEquals(transactionExpired.getTrxCode(), expiredTrxResult.getTrxCode());
    Assertions.assertEquals(transactionExpired.getTrxDate().toInstant(), expiredTrxResult.getTrxDate().toInstant());
    Assertions.assertEquals(transactionExpired.getIdTrxAcquirer(), expiredTrxResult.getIdTrxAcquirer());
    Assertions.assertEquals(transactionExpired.getOperationType(), expiredTrxResult.getOperationType());

    Assertions.assertNull(
        transactionInProgressRepository.findCancelExpiredTransaction("DUMMYINITIATIVEID",
            EXPIRATION_MINUTES));

    TransactionInProgress expiredTrxThrottledResult = transactionInProgressRepository.findCancelExpiredTransaction(
        null, EXPIRATION_MINUTES);
    Assertions.assertNull(expiredTrxThrottledResult);
  }


  @Test
  void testFindCancelExpiredTransaction_concurrent() {
    TransactionInProgress transactionExpired =
        TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
    transactionExpired.setTrxDate(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
    transactionInProgressRepository.save(transactionExpired);

    executeConcurrentLocks(10,
        () -> transactionInProgressRepository.findCancelExpiredTransaction(null, EXPIRATION_MINUTES)
            != null);
  }

  @Test
  void deletePaged() {
    // Given
    int pageSize = 100;
    TransactionInProgress transactionInProgress = TransactionInProgress.builder()
        .id(TRX_ID)
        .initiativeId(INITIATIVE_ID)
        .initiatives(List.of(INITIATIVE_ID))
        .counterVersion(0L)
        .build();
    mongoTemplate.save(transactionInProgress);

    // When
    List<TransactionInProgress> result = transactionInProgressRepository.deletePaged(INITIATIVE_ID,
        pageSize);

    // Then
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(transactionInProgress.getId(), result.get(0).getId());
    Assertions.assertEquals(transactionInProgress.getInitiativeId(),
        result.get(0).getInitiativeId());
  }

  @Test
  void updateTrxPostTimeout_OK() {

    TransactionInProgress transactionInProgress = TransactionInProgress.builder()
        .id(TRX_ID)
        .initiativeId(INITIATIVE_ID)
        .initiatives(List.of(INITIATIVE_ID))
        .status(SyncTrxStatus.AUTHORIZATION_REQUESTED)
        .counterVersion(0L)
        .build();
    mongoTemplate.save(transactionInProgress);

    // When
    UpdateResult result = transactionInProgressRepository.updateTrxPostTimeout(TRX_ID);

    assertNull(result.getUpsertedId());
    assertEquals(1L, result.getMatchedCount());
    assertEquals(1L, result.getModifiedCount());

  }

  @Test
  void updateTrxPostTimeout_KO() {

    TransactionInProgress transactionInProgress = TransactionInProgress.builder()
        .id(TRX_ID)
        .initiativeId(INITIATIVE_ID)
        .initiatives(List.of(INITIATIVE_ID))
        .status(SyncTrxStatus.REJECTED)
        .counterVersion(0L)
        .build();
    mongoTemplate.save(transactionInProgress);

    // When
    UpdateResult result = transactionInProgressRepository.updateTrxPostTimeout(TRX_ID);

    assertNull(result.getUpsertedId());
    assertEquals(0L, result.getMatchedCount());
    assertEquals(0L, result.getModifiedCount());

  }

  @Test
  void updateTrxRejected_barCodeChannel() {
    TransactionInProgress transactionInProgress2 =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgress2.setUserId(USER_ID);
    transactionInProgress2.setChannel(RewardConstants.TRX_CHANNEL_BARCODE);

    TransactionInProgress transactionInProgress =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgress.setUserId(USER_ID);
    transactionInProgress.setChannel(RewardConstants.TRX_CHANNEL_BARCODE);
    transactionInProgress.setAcquirerId(null);
    transactionInProgress.setAmountCents(null);
    transactionInProgress.setEffectiveAmountCents(null);
    transactionInProgress.setAmountCurrency(null);
    transactionInProgress.setMerchantFiscalCode(null);
    transactionInProgress.setMerchantId(null);
    transactionInProgress.setIdTrxAcquirer(null);
    transactionInProgress.setIdTrxIssuer(null);
    transactionInProgress.setMcc(null);
    transactionInProgress.setBusinessName(null);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(
        resultFirstSave,
        "authDate", "elaborationDateTime", "rewardCents", "rejectionReasons", "rewards",
        "trxChargeDate",
        "acquirerId", "amountCents", "effectiveAmountCents", "amountCurrency", "merchantFiscalCode",
        "merchantId", "invoiceData", "creditNoteData",
        "idTrxAcquirer", "idTrxIssuer", "mcc", "businessName", "initiativeRejectionReasons",
        "initiativeEndDate", "voucherAmountCents", "franchiseName", "pointOfSaleType", "familyId");

    transactionInProgressRepository.updateTrxRejected(transactionInProgress2,
        List.of("REJECTIONREASON1"),
        Map.of(transactionInProgress.getInitiativeId(), List.of("REJECTIONREASON1")));

    TransactionInProgress resultSecondSave =
        transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave,
        "authDate", "elaborationDateTime", "reward", "rewards", "trxChargeDate", "idTrxIssuer",
        "mcc", "initiativeEndDate", "voucherAmountCents", "invoiceData", "creditNoteData",
        "franchiseName", "pointOfSaleType", "familyId");
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
  }

  @Test
    void updateTrxExpiredIfInitiativeEnded() {
      OffsetDateTime initEndDate = OffsetDateTime.now().minusDays(1);
      OffsetDateTime trxEndDate  = OffsetDateTime.now().plusDays(1);

      TransactionInProgress trx =
              TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
      trx.setInitiativeEndDate(initEndDate);
      trx.setTrxEndDate(trxEndDate);
      trx.setInitiativeId("INIT_1");
      trx.setExtendedAuthorization(true);
      trx.setUserId("USER_1");
      transactionInProgressRepository.save(trx);

      TransactionInProgress before =
              transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.CREATED, before.getStatus());

      transactionInProgressRepository.updateStatusForExpiredVoucherTransactions("INIT_1");

      TransactionInProgress after =
              transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.EXPIRED, after.getStatus());
    }

    @Test
    void findStaleExpired() {
        OffsetDateTime updateTime = OffsetDateTime.now();
        updateTime = updateTime.minusMinutes(120);
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.EXPIRED);
        transactionInProgress.setInitiativeEndDate(updateTime);
        transactionInProgress.setTrxEndDate(updateTime);
        transactionInProgress.setUpdateDate(updateTime.toLocalDateTime());
        transactionInProgress.setInitiativeId(transactionInProgress.getId());
        transactionInProgress.setExtendedAuthorization(true);

        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultSave);
        Assertions.assertEquals(SyncTrxStatus.EXPIRED, resultSave.getStatus());
        List<TransactionInProgress> transactionInProgresses =
                transactionInProgressRepository.findUnprocessedExpiredVoucherTransactions(
                transactionInProgress.getInitiativeId(),1,0);

        TransactionInProgress resultAfterUpdate =
                transactionInProgresses.get(0);
        Assertions.assertNotNull(resultAfterUpdate);
        Assertions.assertEquals(SyncTrxStatus.EXPIRED, resultAfterUpdate.getStatus());

        transactionInProgresses =
                transactionInProgressRepository.findUnprocessedExpiredVoucherTransactions(
                        transactionInProgress.getInitiativeId(),1,2);
        Assertions.assertTrue(transactionInProgresses.isEmpty());

    }

  private Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> executeConcurrentLocks(
      int attempts, Supplier<Boolean> lockAcquirer) {
    AtomicInteger dropped = new AtomicInteger(0);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      MongoTestUtilitiesService.startMongoCommandListener("lockingQuery_Concurrent");
      List<Future<Integer>> tasks =
          IntStream.range(0, attempts)
              .mapToObj(i -> executorService.submit(() -> {
                if (lockAcquirer.get()) {
                  return 1;
                } else {
                  dropped.incrementAndGet();
                  return 0;
                }
              }))
              .toList();
      int successfulLocks = tasks.stream().mapToInt(f -> {
        try {
          return f.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new IllegalStateException(e);
        }
      }).sum();
      List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>> commands = MongoTestUtilitiesService.stopAndGetMongoCommands();
      MongoTestUtilitiesService.printMongoCommands(commands);

      Assertions.assertEquals(1, successfulLocks);
      Assertions.assertEquals(attempts - 1, dropped.get());

      Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> groupByCommand = commands.stream()
          .collect(Collectors.groupingBy(c -> c.getKey().getType()));
      Assertions.assertEquals(attempts, groupByCommand.get("findAndModify").get(0).getValue());

      return groupByCommand;
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  void findPendingTransactions() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oldDate = now.minusHours(25);
    LocalDateTime recentDate = now.minusHours(1);

    TransactionInProgress recentAuthorized = TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.AUTHORIZED);
    recentAuthorized.setUpdateDate(recentDate);
    transactionInProgressRepository.save(recentAuthorized);

    TransactionInProgress oldAuthorized = TransactionInProgressFaker.mockInstance(4, SyncTrxStatus.AUTHORIZED);
    oldAuthorized.setUpdateDate(oldDate);
    transactionInProgressRepository.save(oldAuthorized);

    TransactionInProgress oldAuthorized2 = TransactionInProgressFaker.mockInstance(5, SyncTrxStatus.AUTHORIZED);
    oldAuthorized2.setUpdateDate(oldDate);
    transactionInProgressRepository.save(oldAuthorized2);

    List<TransactionInProgress> result = transactionInProgressRepository.findPendingTransactions(10);

    assertNotNull(result);
    assertEquals(2, result.size());

    List<String> resultIds = result.stream()
        .map(TransactionInProgress::getId)
        .toList();
    assertTrue(resultIds.contains(oldAuthorized.getId()));
    assertTrue(resultIds.contains(oldAuthorized2.getId()));

    assertFalse(resultIds.contains(recentAuthorized.getId()));

    result.forEach(trx -> {
      assertNotNull(trx.getId());
      assertNotNull(trx.getTrxCode());
      assertNotNull(trx.getMerchantId());
      assertNotNull(trx.getAcquirerId());
    });
  }
  @Test
  void findLapsedTransactionAndDelete() {
    OffsetDateTime updateTime = OffsetDateTime.now();
    updateTime = updateTime.minusMinutes(120);
    TransactionInProgress transactionInProgress =
            TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
    transactionInProgress.setInitiativeEndDate(updateTime);
    transactionInProgress.setTrxEndDate(updateTime);
    transactionInProgress.setUpdateDate(updateTime.toLocalDateTime());
    transactionInProgress.setInitiativeId(transactionInProgress.getId());
    transactionInProgress.setExtendedAuthorization(false);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSave =
            transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSave);
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSave.getStatus());
    List<TransactionInProgress> transactionInProgresses =
            transactionInProgressRepository.findLapsedTransaction(transactionInProgress.getInitiativeId(),100);

    TransactionInProgress resultAfterUpdate =
            transactionInProgresses.get(0);
    Assertions.assertNotNull(resultAfterUpdate);
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultAfterUpdate.getStatus());
  }

  @Test
  void bulkDeleteByIds() {
    OffsetDateTime updateTime = OffsetDateTime.now();
    updateTime = updateTime.minusMinutes(120);
    TransactionInProgress transactionInProgress =
            TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
    transactionInProgress.setInitiativeEndDate(updateTime);
    transactionInProgress.setTrxEndDate(updateTime);
    transactionInProgress.setUpdateDate(updateTime.toLocalDateTime());
    transactionInProgress.setInitiativeId(transactionInProgress.getId());
    transactionInProgress.setExtendedAuthorization(false);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSave =
            transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSave);
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSave.getStatus());
    transactionInProgressRepository.bulkDeleteByIds(List.of(resultSave.getId()));

    TransactionInProgress resultAfterUpdate =
            transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNull(resultAfterUpdate);

  }

  @Test
  void updateTrxExpiredIfTrxEndDatePassed() {
      OffsetDateTime initEndDate = OffsetDateTime.now().plusDays(10);
      OffsetDateTime trxEndDate  = OffsetDateTime.now().minusMinutes(1);
      TransactionInProgress trx = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
      trx.setInitiativeEndDate(initEndDate);
      trx.setTrxEndDate(trxEndDate);
      trx.setInitiativeId("INIT_2");
      trx.setExtendedAuthorization(true);
      trx.setUserId("USER_2");
      transactionInProgressRepository.save(trx);
      transactionInProgressRepository.updateStatusForExpiredVoucherTransactions("INIT_2");
      TransactionInProgress after =
              transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_2").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.EXPIRED, after.getStatus());
  }

  @Test
  void doNotExpireIfExtendedAuthorizationIsFalse() {
      OffsetDateTime initEndDate = OffsetDateTime.now().minusDays(1);
      OffsetDateTime trxEndDate  = OffsetDateTime.now().minusMinutes(1);
      TransactionInProgress trx = TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.CREATED);
      trx.setInitiativeEndDate(initEndDate);
      trx.setTrxEndDate(trxEndDate);
      trx.setInitiativeId("INIT_3");
      trx.setExtendedAuthorization(false);
      trx.setUserId("USER_3");
      transactionInProgressRepository.save(trx);
      transactionInProgressRepository.updateStatusForExpiredVoucherTransactions("INIT_3");
      TransactionInProgress after = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_3").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.CREATED, after.getStatus());
    }

  @Test
  void doNotExpireIfUserHasAuthorizedTransaction() {
      String initiativeId = "INIT_4";
      String userId = "USER_4";

      // Candidate expired (CREATED) -> should be SKIPPED
      TransactionInProgress created = TransactionInProgressFaker.mockInstance(4, SyncTrxStatus.CREATED);
      created.setInitiativeId(initiativeId);
      created.setUserId(userId);
      created.setExtendedAuthorization(true);
      created.setTrxEndDate(OffsetDateTime.now().minusMinutes(1));
      created.setInitiativeEndDate(OffsetDateTime.now().plusDays(1));
      transactionInProgressRepository.save(created);

      // AUTHORIZED for same user -> lock l’expire
      TransactionInProgress authorized =
              TransactionInProgressFaker.mockInstance(5, SyncTrxStatus.AUTHORIZED);
      authorized.setInitiativeId(initiativeId);
      authorized.setUserId(userId);
      transactionInProgressRepository.save(authorized);

      transactionInProgressRepository.updateStatusForExpiredVoucherTransactions(initiativeId);

      TransactionInProgress after = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_4").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.CREATED, after.getStatus());
  }

  @Test
  void shouldAdvanceCursorWhenFirstBatchIsAllSkippedDueToAuthorizedUsers() {
      String initiativeId = "INIT_5";
      // User A: have all AUTHORIZED trx -> all his CREATED trx should be skipped
      String userA = "USER_A";
      TransactionInProgress authA = TransactionInProgressFaker.mockInstance(10, SyncTrxStatus.AUTHORIZED);
      authA.setInitiativeId(initiativeId);
      authA.setUserId(userA);
      transactionInProgressRepository.save(authA);

      // Create more CREATED
      for (int i = 11; i <= 20; i++) {
          TransactionInProgress t = TransactionInProgressFaker.mockInstance(i, SyncTrxStatus.CREATED);
          t.setInitiativeId(initiativeId);
          t.setUserId(userA);
          t.setExtendedAuthorization(true);
          t.setTrxEndDate(OffsetDateTime.now().minusMinutes(1));
          t.setInitiativeEndDate(OffsetDateTime.now().plusDays(1));
          transactionInProgressRepository.save(t);
      }

      // User B: Not AUTHORIZED -> his CREATED should be EXPIRED
      String userB = "USER_B";
      TransactionInProgress tB = TransactionInProgressFaker.mockInstance(21, SyncTrxStatus.CREATED);
      tB.setInitiativeId(initiativeId);
      tB.setUserId(userB);
      tB.setExtendedAuthorization(true);
      tB.setTrxEndDate(OffsetDateTime.now().minusMinutes(1));
      tB.setInitiativeEndDate(OffsetDateTime.now().plusDays(1));
      transactionInProgressRepository.save(tB);

      transactionInProgressRepository.updateStatusForExpiredVoucherTransactions(initiativeId);

      TransactionInProgress afterB = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_21").orElseThrow();
      Assertions.assertEquals(SyncTrxStatus.EXPIRED, afterB.getStatus());
    }
}

