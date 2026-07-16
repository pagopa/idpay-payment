package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionSpecifications {

    public static Specification<Transaction> buildSpecification(TrxFiltersDTO filters, String encryptedUserId) {
        return Specification
                .where(TransactionSpecifications.hasStatus(filters.getStatus()))
                .and(TransactionSpecifications.hasTrxCode(filters.getTrxCode()))
                .and(TransactionSpecifications.hasMerchantId(filters.getMerchantId()))
                .and(TransactionSpecifications.hasInitiativeId(filters.getInitiativeId()))
                .and(TransactionSpecifications.hasFiscalCode(encryptedUserId))
                .and(TransactionSpecifications.hasRewardBatchId(filters.getRewardBatchId()))
                .and(TransactionSpecifications.hasRewardBatchTrxStatus(filters.getRewardBatchTrxStatus()))
                .and(TransactionSpecifications.hasPointOfSaleId(filters.getPointOfSaleId()))
                .and(TransactionSpecifications.hasProductGtin(filters.getProductGtin()));
    }

    public static Specification<Transaction> getFilters(
            TrxFiltersDTO filters,
            String userId) {

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
                if (filters.isIncludeToCheckWithConsultable()) {
                    predicates.add(root.get("rewardBatchTrxStatus").in(
                            RewardBatchTrxStatus.CONSULTABLE,
                            RewardBatchTrxStatus.TO_CHECK
                    ));
                } else {
                    predicates.add(cb.equal(root.get("rewardBatchTrxStatus"), filters.getRewardBatchTrxStatus()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> hasStatus(String statusStr) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(statusStr)) {
                return criteriaBuilder.conjunction();
            }
            try {
                SyncTrxStatus statusEnum = SyncTrxStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
                return criteriaBuilder.equal(root.get("status"), statusEnum);
            } catch (IllegalArgumentException e) {
                return criteriaBuilder.disjunction();
            }
        };
    }

    public static Specification<Transaction> hasTrxCode(String trxCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(trxCode)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("trxCode"), trxCode);
        };
    }

    public static Specification<Transaction> hasMerchantId(String merchantId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(merchantId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("merchantId"), merchantId);
        };
    }

    public static Specification<Transaction> hasInitiativeId(String initiativeId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(initiativeId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("initiativeId"), initiativeId);
        };
    }

    public static Specification<Transaction> hasFiscalCode(String fiscalCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(fiscalCode)) {
                return criteriaBuilder.conjunction();
            }
            // Mappato su merchantFiscalCode dell'entità
            return criteriaBuilder.equal(root.get("merchantFiscalCode"), fiscalCode);
        };
    }

    public static Specification<Transaction> hasRewardBatchId(String rewardBatchId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(rewardBatchId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("rewardBatchId"), rewardBatchId);
        };
    }

    public static Specification<Transaction> hasRewardBatchTrxStatus(RewardBatchTrxStatus rewardBatchTrxStatus) {
        return (root, query, criteriaBuilder) -> {
            if (rewardBatchTrxStatus == null) {
                return criteriaBuilder.conjunction();
            }
            // L'entità ha una String (rewardBatchStatusTrx), quindi passiamo il name() dell'enum
            return criteriaBuilder.equal(root.get("rewardBatchStatusTrx"), rewardBatchTrxStatus.name());
        };
    }

    public static Specification<Transaction> hasPointOfSaleId(String pointOfSaleId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(pointOfSaleId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("pointOfSaleId"), pointOfSaleId);
        };
    }

    public static Specification<Transaction> hasProductGtin(String productGtin) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(productGtin)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("additionalProperties"),
                            criteriaBuilder.literal("productGtin")
                    ),
                    productGtin
            );
        };
    }

    private static void addTextPredicate(List<Predicate> predicates,
                                         Root<Transaction> root,
                                         CriteriaBuilder criteriaBuilder,
                                         String field,
                                         String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(criteriaBuilder.equal(root.get(field), value));
        }
    }
}