package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class TransactionSpecifications {

    public static Specification<Transaction> hasStatus(String statusStr) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(statusStr)) {
                return criteriaBuilder.conjunction();
            }
            try {
                SyncTrxStatus statusEnum = SyncTrxStatus.valueOf(statusStr.toUpperCase());
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
}