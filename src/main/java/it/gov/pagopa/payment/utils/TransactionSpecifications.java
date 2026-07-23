package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
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
                predicates.add(cb.equal(root.get("amountCents"), amountCents));
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
                predicates.add(cb.equal(root.get("amountCents"), amountCents));
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

    public static Specification<Transaction> buildSpecification(TrxFiltersDTO filters, String encryptedUserId) {
        return Specification
                .where(hasStatuses(filters.getStatuses()))
                .and(hasTrxCode(filters.getTrxCode()))
                .and(hasMerchantId(filters.getMerchantId()))
                .and(hasInitiativeId(filters.getInitiativeId()))
                .and(hasFiscalCode(encryptedUserId))
                .and(hasRewardBatchId(filters.getRewardBatchId()))
                .and(hasRewardBatchTrxStatus(filters.getRewardBatchTrxStatus()))
                .and(hasPointOfSaleId(filters.getPointOfSaleId()))
                .and(hasProductGtin(filters.getProductGtin()));
    }

    public static Specification<Transaction> getFilters(TrxFiltersDTO filters, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addTextPredicate(predicates, root, cb, FIELD_MERCHANT_ID, filters.getMerchantId());
            addTextPredicate(predicates, root, cb, FIELD_INITIATIVE_ID, filters.getInitiativeId());
            addTextPredicate(predicates, root, cb, FIELD_USER_ID, userId);
            addTextPredicate(predicates, root, cb, FIELD_POINT_OF_SALE_ID, filters.getPointOfSaleId());
            addTextPredicate(predicates, root, cb, FIELD_TRX_CODE, filters.getTrxCode());
            addTextPredicate(predicates, root, cb, FIELD_REWARD_BATCH_ID, filters.getRewardBatchId());

            handleStatusFilters(predicates, root, cb, filters);

            if (filters.getRewardBatchTrxStatus() != null) {
                Path<String> statusField = root.get(FIELD_REWARD_BATCH_STATUS_TRX);
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
        return (root, query, cb) -> StringUtils.hasText(fiscalCode) ? cb.equal(root.get("merchantFiscalCode"), fiscalCode) : cb.conjunction();
    }

    public static Specification<Transaction> hasRewardBatchId(String rewardBatchId) {
        return (root, query, cb) -> StringUtils.hasText(rewardBatchId) ? cb.equal(root.get(FIELD_REWARD_BATCH_ID), rewardBatchId) : cb.conjunction();
    }

    public static Specification<Transaction> hasRewardBatchTrxStatus(RewardBatchTrxStatus status) {
        return (root, query, cb) -> status != null ? cb.equal(root.get(FIELD_REWARD_BATCH_STATUS_TRX), status.name()) : cb.conjunction();
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
                            cb.literal("productGtin")
                    ),
                    productGtin
            );
        };
    }

    private static void addTextPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder cb,
            String field,
            String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(cb.equal(root.get(field), value));
        }
    }
}