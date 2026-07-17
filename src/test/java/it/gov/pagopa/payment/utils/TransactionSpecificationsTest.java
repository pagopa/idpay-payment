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
        // Comportamento standard per evitare NullPointerException durante la navigazione dei path
        lenient().when(root.get(any(String.class))).thenReturn(path);
    }

    // =========================================================================
    // 1. TEST: findByInitiativeAndUser
    // =========================================================================
    @Test
    void findByInitiativeAndUser_generatesCorrectPredicates() {
        String initiativeId = "INIT_123";
        String userId = "USER_456";

        Predicate eqUserId = mock(Predicate.class);
        Predicate isMember = mock(Predicate.class);
        Predicate finalAnd = mock(Predicate.class);

        when(cb.equal(path, userId)).thenReturn(eqUserId);
        when(cb.isMember(eq(initiativeId), any(Expression.class))).thenReturn(isMember);
        when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

        Specification<Transaction> spec = TransactionSpecifications.findByInitiativeAndUser(initiativeId, userId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(finalAnd, result);
        verify(root).get("userId");
        verify(root).get("initiatives");
    }

    // =========================================================================
    // 2. TEST: findByRangeFilters
    // =========================================================================
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

        // Verifica che "amountCents" non sia mai stato richiesto al root
        verify(root, never()).get("amountCents");
    }

    // =========================================================================
    // 3. TEST: findByIssuerFilters
    // =========================================================================
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

    // =========================================================================
    // 4. TEST: buildSpecification (Chiamate concatenate delle specifiche singole)
    // =========================================================================
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

        // buildSpecification concatena con .and() / .where()
        // Verifichiamo che non sia nullo (la logica di combinazione è gestita internamente da Spring Data JPA)
        assertNotNull(spec);
    }

    // =========================================================================
    // 5. TEST: getFilters
    // =========================================================================
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

    // =========================================================================
    // 6. TEST: Specifiche di base (hasStatus, hasProductGtin, ecc.)
    // =========================================================================
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
    void hasProductGtin_emptyValue_returnsConjunction() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Transaction> spec = TransactionSpecifications.hasProductGtin(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(conjunction, result);
        verify(cb, never()).function(any(), any(), any(), any());
    }
}