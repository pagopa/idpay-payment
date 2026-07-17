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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> findByInitiativeAndUser(String initiativeId, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isMember(initiativeId, root.get("initiatives")));
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
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.between(root.get("trxDate"), trxDateStart, trxDateEnd));
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
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (amountCents != null) {
                predicates.add(cb.equal(root.get("amountCents"), amountCents));
            }
            if (trxDateStart != null && trxDateEnd != null) {
                predicates.add(cb.between(root.get("trxDate"), trxDateStart, trxDateEnd));
            } else if (trxDateStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("trxDate"), trxDateStart));
            } else if (trxDateEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("trxDate"), trxDateEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> buildSpecification(TrxFiltersDTO filters, String encryptedUserId) {
        return Specification
                .where(hasStatus(filters.getStatus()))
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

            addTextPredicate(predicates, root, cb, "merchantId", filters.getMerchantId());
            addTextPredicate(predicates, root, cb, "initiativeId", filters.getInitiativeId());
            addTextPredicate(predicates, root, cb, "userId", userId);
            addTextPredicate(predicates, root, cb, "pointOfSaleId", filters.getPointOfSaleId());
            addTextPredicate(predicates, root, cb, "trxCode", filters.getTrxCode());
            addTextPredicate(predicates, root, cb, "rewardBatchId", filters.getRewardBatchId());
            addTextPredicate(predicates, root, cb, "status", filters.getStatus());

            if (filters.getRewardBatchTrxStatus() != null) {
                Path<String> statusField = root.get("rewardBatchStatusTrx");
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

    public static Specification<Transaction> hasStatus(String statusStr) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(statusStr)) {
                return cb.conjunction();
            }
            try {
                SyncTrxStatus statusEnum = SyncTrxStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
                return cb.equal(root.get("status"), statusEnum);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Transaction> hasTrxCode(String trxCode) {
        return (root, query, cb) -> StringUtils.hasText(trxCode) ? cb.equal(root.get("trxCode"), trxCode) : cb.conjunction();
    }

    public static Specification<Transaction> hasMerchantId(String merchantId) {
        return (root, query, cb) -> StringUtils.hasText(merchantId) ? cb.equal(root.get("merchantId"), merchantId) : cb.conjunction();
    }

    public static Specification<Transaction> hasInitiativeId(String initiativeId) {
        return (root, query, cb) -> StringUtils.hasText(initiativeId) ? cb.equal(root.get("initiativeId"), initiativeId) : cb.conjunction();
    }

    public static Specification<Transaction> hasFiscalCode(String fiscalCode) {
        return (root, query, cb) -> StringUtils.hasText(fiscalCode) ? cb.equal(root.get("merchantFiscalCode"), fiscalCode) : cb.conjunction();
    }

    public static Specification<Transaction> hasRewardBatchId(String rewardBatchId) {
        return (root, query, cb) -> StringUtils.hasText(rewardBatchId) ? cb.equal(root.get("rewardBatchId"), rewardBatchId) : cb.conjunction();
    }

    public static Specification<Transaction> hasRewardBatchTrxStatus(RewardBatchTrxStatus status) {
        return (root, query, cb) -> status != null ? cb.equal(root.get("rewardBatchStatusTrx"), status.name()) : cb.conjunction();
    }

    public static Specification<Transaction> hasPointOfSaleId(String pointOfSaleId) {
        return (root, query, cb) -> StringUtils.hasText(pointOfSaleId) ? cb.equal(root.get("pointOfSaleId"), pointOfSaleId) : cb.conjunction();
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