package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSpecificationsTest {

    @Mock
    private Root<Transaction> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(any(String.class))).thenReturn(path);
    }

    @Test
    void findByInitiativeAndUser_success() {
        String initiativeId = "INIT_1";
        String userId = "USER_1";

        Predicate eqUser = mock(Predicate.class);
        Predicate eqInit = mock(Predicate.class);
        Predicate finalAnd = mock(Predicate.class);

        when(cb.equal(root.get("userId"), userId)).thenReturn(eqUser);
        when(cb.equal(root.get("initiativeId"), initiativeId)).thenReturn(eqInit);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByInitiativeAndUser(initiativeId, userId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(finalAnd, result);
    }


    @Test
    void findByRangeFilters_withAmount_generatesAllPredicates() {
        String userId = "USER_1";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Long amount = 1500L;

        Predicate eqUser = mock(Predicate.class);
        Predicate betweenDates = mock(Predicate.class);
        Predicate eqAmount = mock(Predicate.class);
        Predicate finalAnd = mock(Predicate.class);

        when(cb.equal(root.get("userId"), userId)).thenReturn(eqUser);
        when(cb.between(any(), eq(start), eq(end))).thenReturn(betweenDates);
        when(cb.equal(root.get("amountCents"), amount)).thenReturn(eqAmount);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByRangeFilters(userId, start, end, amount);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(finalAnd, result);
    }

    @Test
    void findByRangeFilters_withoutAmount_omitsAmountPredicate() {
        String userId = "USER_1";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        Predicate finalAnd = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByRangeFilters(userId, start, end, null);
        spec.toPredicate(root, query, cb);

        verify(root, never()).get("amountCents");
    }


    @Test
    void findByIssuerFilters_withAllParameters() {
        String issuer = "ISSUER_1";
        String userId = "USER_1";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Long amount = 500L;

        Predicate finalAnd = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByIssuerFilters(issuer, userId, start, end, amount);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("idTrxIssuer");
        verify(root).get("userId");
        verify(root).get("amountCents");
        verify(cb).between(any(), eq(start), eq(end));
    }

    @Test
    void findByIssuerFilters_onlyStartDate() {
        LocalDateTime start = LocalDateTime.now();
        Predicate finalAnd = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByIssuerFilters("ISSUER_1", null, start, null, null);
        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(any(), eq(start));
    }

    @Test
    void findByIssuerFilters_onlyEndDate() {
        LocalDateTime end = LocalDateTime.now();
        Predicate finalAnd = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByIssuerFilters("ISSUER_1", null, null, end, null);
        spec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(any(), eq(end));
    }

    @Test
    void findByIssuerFilters_withoutDatesOrUser() {
        Predicate finalAnd = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByIssuerFilters("ISSUER_1", "", null, null, null);
        spec.toPredicate(root, query, cb);

        verify(root, never()).get("userId");
    }

    @Test
    void buildSpecification_combinesAllFilters() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setStatuses(List.of("REWARDED"));
        filters.setTrxCode("CODE123");
        filters.setMerchantId("MERCH1");
        filters.setInitiativeId("INIT1");
        filters.setRewardBatchId("BATCH1");
        filters.setRewardBatchTrxStatus(RewardBatchTrxStatus.CONSULTABLE);
        filters.setPointOfSaleId("POS1");
        filters.setProductGtin("GTIN123");

        Specification<Transaction> spec = TransactionSpecifications.buildSpecification(filters, "ENCRYPTED_FC");
        assertNotNull(spec);
    }

    @Test
    void getFilters_withIncludeToCheckWithConsultable_true() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setMerchantId("MERCH_1");
        filters.setRewardBatchTrxStatus(RewardBatchTrxStatus.CONSULTABLE);
        filters.setIncludeToCheckWithConsultable(true);

        CriteriaBuilder.In<Object> mockIn = mock(CriteriaBuilder.In.class);
        when(path.in(any(Object[].class))).thenReturn(mockIn);

        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, "USER_1");
        spec.toPredicate(root, query, cb);

        verify(root).get("rewardBatchStatusTrx");
        verify(path).in(RewardBatchTrxStatus.CONSULTABLE.name(), RewardBatchTrxStatus.TO_CHECK.name());
    }

    @Test
    void getFilters_withIncludeToCheckWithConsultable_false() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setRewardBatchTrxStatus(RewardBatchTrxStatus.CONSULTABLE);
        filters.setIncludeToCheckWithConsultable(false);

        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, "USER_1");
        spec.toPredicate(root, query, cb);

        verify(cb).equal(path, RewardBatchTrxStatus.CONSULTABLE.name());
    }

    @Test
    void getFilters_withStatusesList() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setStatuses(List.of("REWARDED", "INVALID_ENUM_SHOULD_BE_FILTERED"));

        CriteriaBuilder.In<Object> mockIn = mock(CriteriaBuilder.In.class);
        when(path.in(any(List.class))).thenReturn(mockIn);

        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, "USER_1");
        spec.toPredicate(root, query, cb);

        verify(root).get("status");
    }

    @Test
    void getFilters_withStatusesListAllInvalid_returnsDisjunction() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setStatuses(List.of("INVALID_STATUS_A", "INVALID_STATUS_B"));

        Predicate disjunction = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(disjunction);

        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, "USER_1");
        spec.toPredicate(root, query, cb);

        verify(cb).disjunction();
    }


    @Test
    void getFilters_withSingleStatusInvalid_returnsDisjunction() {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setStatuses(List.of("UNKNOWN_STATUS"));

        Predicate disjunction = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(disjunction);

        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, "USER_1");
        spec.toPredicate(root, query, cb);

        verify(cb).disjunction();
    }

    @Test
    void hasStatuses_validStatuses_returnsInPredicate() {
        List<String> statuses = List.of("REWARDED", "AUTHORIZED");
        CriteriaBuilder.In<Object> mockIn = mock(CriteriaBuilder.In.class);
        when(path.in(any(List.class))).thenReturn(mockIn);

        Specification<Transaction> spec = TransactionSpecifications.hasStatuses(statuses);
        spec.toPredicate(root, query, cb);

        verify(root).get("status");
    }

    @Test
    void hasStatuses_invalidStatuses_returnsDisjunction() {
        List<String> statuses = List.of("NOT_A_STATUS");
        Predicate disjunction = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(disjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasStatuses(statuses);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(disjunction, result);
    }

    @Test
    void hasStatuses_emptyStatuses_returnsConjunction() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasStatuses(Collections.emptyList());
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(conjunction, result);
    }

    @Test
    void hasStatus_validStatus_returnsEqualPredicate() {
        Specification<Transaction> spec = TransactionSpecifications.hasStatus("REWARDED");
        spec.toPredicate(root, query, cb);

        verify(cb).equal(path, SyncTrxStatus.REWARDED);
    }

    @Test
    void hasStatus_invalidStatus_returnsDisjunction() {
        Predicate disjunction = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(disjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasStatus("INVALID_STATUS_XYZ");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(disjunction, result);
    }

    @Test
    void hasStatus_emptyStatus_returnsConjunction() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasStatus("");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(conjunction, result);
    }

    @Test
    void hasTrxCode_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasTrxCode("TX123");
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "TX123");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasTrxCode("");
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasMerchantId_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasMerchantId("M1");
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "M1");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasMerchantId(null);
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasInitiativeId_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasInitiativeId("I1");
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "I1");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasInitiativeId(" ");
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasFiscalCode_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasFiscalCode("FC123");
        spec.toPredicate(root, query, cb);
        verify(root).get("merchantFiscalCode");
        verify(cb).equal(path, "FC123");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasFiscalCode(null);
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasRewardBatchId_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasRewardBatchId("B1");
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "B1");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasRewardBatchId("");
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasRewardBatchTrxStatus_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasRewardBatchTrxStatus(RewardBatchTrxStatus.CONSULTABLE);
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "CONSULTABLE");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasRewardBatchTrxStatus(null);
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasPointOfSaleId_validAndEmpty() {
        Specification<Transaction> spec = TransactionSpecifications.hasPointOfSaleId("POS1");
        spec.toPredicate(root, query, cb);
        verify(cb).equal(path, "POS1");

        reset(cb);
        Specification<Transaction> specEmpty = TransactionSpecifications.hasPointOfSaleId(null);
        specEmpty.toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void hasProductGtin_validValue_returnsFunctionCall() {
        Expression<String> mockFunction = mock(Expression.class);
        when(cb.function(eq("jsonb_extract_path_text"), eq(String.class), any(), any())).thenReturn(mockFunction);

        Specification<Transaction> spec = TransactionSpecifications.hasProductGtin("GTIN_999");
        spec.toPredicate(root, query, cb);

        verify(cb).function(
                eq("jsonb_extract_path_text"),
                eq(String.class),
                eq(path),
                any()
        );
        verify(cb).equal(mockFunction, "GTIN_999");
    }

    @Test
    void hasProductGtin_emptyValue_returnsConjunction() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasProductGtin(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(conjunction, result);
        verify(cb, never()).function(any(), any(), any(), any());
    }
}