package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.RewardTransaction;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionSearchMode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TransactionSpecifications {

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_TRX_DATE = "trxDate";
    private static final String FIELD_STATUS = "status";

    private static final String FIELD_INITIATIVE_ID = "initiativeId";
    private static final String FIELD_MERCHANT_ID = "merchantId";
    private static final String FIELD_POINT_OF_SALE_ID = "pointOfSaleId";
    private static final String FIELD_TRX_CODE = "trxCode";
    private static final String FIELD_REWARD_BATCH_ID = "rewardBatchId";
    private static final String FIELD_REWARD_BATCH_STATUS_TRX = "rewardBatchStatusTrx";
    private static final String FIELD_REWARD_TRANSACTION = "rewardTransaction";
    private static final String FIELD_PRODUCT_GTIN = "productGtin";
    private static final String FIELD_AMOUNT_CENTS = "amountCents";
    private TransactionSpecifications() {
    }

    public static Specification<Transaction> findByInitiativeAndUser(String initiativeId, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(FIELD_USER_ID), userId));
            predicates.add(cb.equal(root.get(FIELD_INITIATIVE_ID), initiativeId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> findByRangeFilters(
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(FIELD_USER_ID), userId));
            predicates.add(cb.between(root.get(FIELD_TRX_DATE), trxDateStart, trxDateEnd));
            if (amountCents != null) {
                predicates.add(cb.equal(root.get(FIELD_AMOUNT_CENTS), amountCents));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> findByIssuerFilters(
            String idTrxIssuer,
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("idTrxIssuer"), idTrxIssuer));

            if (StringUtils.hasText(userId)) {
                predicates.add(cb.equal(root.get(FIELD_USER_ID), userId));
            }
            if (amountCents != null) {
                predicates.add(cb.equal(root.get(FIELD_AMOUNT_CENTS), amountCents));
            }
            if (trxDateStart != null && trxDateEnd != null) {
                predicates.add(cb.between(root.get(FIELD_TRX_DATE), trxDateStart, trxDateEnd));
            } else if (trxDateStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_TRX_DATE), trxDateStart));
            } else if (trxDateEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_TRX_DATE), trxDateEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> buildSearchSpecification(TrxFiltersDTO filters, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Transaction, RewardTransaction> rewardTransactionJoin = null;

            if (isProcessedSearch(filters)) {
                rewardTransactionJoin = getOrCreateRewardTransactionJoin(root);
                fetchRewardTransaction(root, query);
            }

            addTextPredicate(predicates, root, cb, FIELD_MERCHANT_ID, filters.getMerchantId());
            addTextPredicate(predicates, root, cb, FIELD_INITIATIVE_ID, filters.getInitiativeId());
            addTextPredicate(predicates, root, cb, FIELD_USER_ID, userId);
            addTextPredicate(predicates, root, cb, FIELD_POINT_OF_SALE_ID, filters.getPointOfSaleId());
            addTextPredicate(predicates, root, cb, FIELD_TRX_CODE, filters.getTrxCode());
            addProductGtinPredicate(predicates, root, cb, filters.getProductGtin());

            handleStatusFilters(predicates, root, cb, filters);

            if (rewardTransactionJoin != null) {
                addTextPredicate(predicates, rewardTransactionJoin, cb, FIELD_REWARD_BATCH_ID, filters.getRewardBatchId());
            }

            if (rewardTransactionJoin != null && filters.getRewardBatchTrxStatus() != null) {
                Path<String> statusField = rewardTransactionJoin.get(FIELD_REWARD_BATCH_STATUS_TRX);
                if (filters.isIncludeToCheckWithConsultable()) {
                    predicates.add(statusField.in(
                            RewardBatchTrxStatus.CONSULTABLE.name(),
                            RewardBatchTrxStatus.TO_CHECK.name()
                    ));
                } else {
                    predicates.add(cb.equal(statusField, filters.getRewardBatchTrxStatus().name()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void handleStatusFilters(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder cb,
            TrxFiltersDTO filters) {

        if (!CollectionUtils.isEmpty(filters.getStatuses())) {
            List<SyncTrxStatus> statusEnums = extractStatusEnums(filters.getStatuses());
            if (!statusEnums.isEmpty()) {
                predicates.add(root.get(FIELD_STATUS).in(statusEnums));
            } else {
                predicates.add(cb.disjunction());
            }
        } else if (StringUtils.hasText(filters.getStatus())) {
            try {
                SyncTrxStatus statusEnum = SyncTrxStatus.valueOf(filters.getStatus().toUpperCase(Locale.ROOT));
                predicates.add(cb.equal(root.get(FIELD_STATUS), statusEnum));
            } catch (IllegalArgumentException _) {
                predicates.add(cb.disjunction());
            }
        }
    }

    private static List<SyncTrxStatus> extractStatusEnums(List<String> statuses) {
        return statuses.stream()
                .map(s -> {
                    try {
                        return SyncTrxStatus.valueOf(s.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException _) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static Specification<Transaction> hasStatuses(List<String> statuses) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(statuses)) {
                return cb.conjunction();
            }
            try {
                List<SyncTrxStatus> statusEnums = statuses.stream()
                        .map(s -> SyncTrxStatus.valueOf(s.toUpperCase(Locale.ROOT)))
                        .toList();
                return root.get(FIELD_STATUS).in(statusEnums);
            } catch (IllegalArgumentException _) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Transaction> hasStatus(String statusStr) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(statusStr)) {
                return cb.conjunction();
            }
            try {
                SyncTrxStatus statusEnum = SyncTrxStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
                return cb.equal(root.get(FIELD_STATUS), statusEnum);
            } catch (IllegalArgumentException _) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Transaction> hasTrxCode(String trxCode) {
        return (root, query, cb) -> StringUtils.hasText(trxCode) ? cb.equal(root.get(FIELD_TRX_CODE), trxCode) : cb.conjunction();
    }

    public static Specification<Transaction> hasMerchantId(String merchantId) {
        return (root, query, cb) -> StringUtils.hasText(merchantId) ? cb.equal(root.get(FIELD_MERCHANT_ID), merchantId) : cb.conjunction();
    }

    public static Specification<Transaction> hasInitiativeId(String initiativeId) {
        return (root, query, cb) -> StringUtils.hasText(initiativeId) ? cb.equal(root.get(FIELD_INITIATIVE_ID), initiativeId) : cb.conjunction();
    }

    public static Specification<Transaction> hasFiscalCode(String fiscalCode) {
        return (root, query, cb) -> StringUtils.hasText(fiscalCode) ? cb.equal(root.get(FIELD_USER_ID), fiscalCode) : cb.conjunction();
    }

    public static Specification<Transaction> hasRewardBatchId(String rewardBatchId) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(rewardBatchId)) {
                return cb.conjunction();
            }
            fetchRewardTransaction(root, query);
            return cb.equal(getOrCreateRewardTransactionJoin(root).get(FIELD_REWARD_BATCH_ID), rewardBatchId);
        };
    }

    public static Specification<Transaction> hasRewardBatchTrxStatus(RewardBatchTrxStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            fetchRewardTransaction(root, query);
            return cb.equal(getOrCreateRewardTransactionJoin(root).get(FIELD_REWARD_BATCH_STATUS_TRX), status.name());
        };
    }

    public static Specification<Transaction> hasPointOfSaleId(String pointOfSaleId) {
        return (root, query, cb) -> StringUtils.hasText(pointOfSaleId) ? cb.equal(root.get(FIELD_POINT_OF_SALE_ID), pointOfSaleId) : cb.conjunction();
    }

    public static Specification<Transaction> hasProductGtin(String productGtin) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(productGtin)) {
                return cb.conjunction();
            }
            return cb.equal(
                    cb.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("additionalProperties"),
                            cb.literal(FIELD_PRODUCT_GTIN)
                    ),
                    productGtin
            );
        };
    }

    private static void addTextPredicate(
            List<Predicate> predicates,
            Path<?> path,
            CriteriaBuilder cb,
            String field,
            String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(cb.equal(path.get(field), value));
        }
    }

    private static boolean isProcessedSearch(TrxFiltersDTO filters) {
        return filters != null && TransactionSearchMode.PROCESSED.equals(filters.getMode());
    }

    private static Join<Transaction, RewardTransaction> getOrCreateRewardTransactionJoin(Root<Transaction> root) {
        return root.getJoins().stream()
                .filter(join -> FIELD_REWARD_TRANSACTION.equals(join.getAttribute().getName()))
                .findFirst()
                .map(join -> (Join<Transaction, RewardTransaction>) join)
                .orElseGet(() -> root.join(FIELD_REWARD_TRANSACTION, JoinType.LEFT));
    }

    private static void fetchRewardTransaction(Root<Transaction> root, jakarta.persistence.criteria.CriteriaQuery<?> query) {
        if (query == null || isCountQuery(query)) {
            return;
        }
        if (root.getFetches().stream().map(Fetch::getAttribute).noneMatch(attribute -> attribute != null && FIELD_REWARD_TRANSACTION.equals(attribute.getName()))) {
            root.fetch(FIELD_REWARD_TRANSACTION, JoinType.LEFT);
        }
        query.distinct(true);
    }

    private static boolean isCountQuery(jakarta.persistence.criteria.CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return (Long.class.equals(resultType) || long.class.equals(resultType));
    }

    private static void addProductGtinPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder cb,
            String productGtin) {
        if (StringUtils.hasText(productGtin)) {
            predicates.add(cb.equal(
                    cb.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("additionalProperties"),
                            cb.literal(FIELD_PRODUCT_GTIN)
                    ),
                    productGtin
            ));
        }
    }
}