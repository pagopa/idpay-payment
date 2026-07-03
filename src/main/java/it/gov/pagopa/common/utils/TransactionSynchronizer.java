package it.gov.pagopa.common.utils;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Component;

@Component
public class TransactionSynchronizer {

    public void sync(
            TransactionInProgress source,
            Transaction target) {

        if (source == null || target == null) {
            return;
        }

        // identifiers
        target.setId(source.getId());
        target.setTrxCode(source.getTrxCode());

        // transaction references
        target.setIdTrxAcquirer(source.getIdTrxAcquirer());
        target.setIdTrxIssuer(source.getIdTrxIssuer());
        target.setCorrelationId(source.getCorrelationId());

        // dates
        target.setTrxDate(source.getTrxDate());
        target.setTrxChargeDate(source.getTrxChargeDate());
        target.setTrxEndDate(source.getTrxEndDate());

        if (source.getElaborationDateTime() != null) {
            target.setElaborationDateTime(source.getElaborationDateTime());
        }

        target.setUpdateDate(source.getUpdateDate());

        // operation
        target.setOperationType(source.getOperationType());
        target.setOperationTypeTranscoded(
                source.getOperationTypeTranscoded());

        target.setStatus(source.getStatus());

        // amounts
        target.setAmountCents(source.getAmountCents());
        target.setEffectiveAmountCents(
                source.getEffectiveAmountCents());

        target.setVoucherAmountCents(
                source.getVoucherAmountCents());

        target.setRewardCents(source.getRewardCents());

        // currency
        target.setAmountCurrency(source.getAmountCurrency());

        // merchant
        target.setMerchantId(source.getMerchantId());
        target.setMerchantFiscalCode(
                source.getMerchantFiscalCode());

        target.setBusinessName(source.getBusinessName());
        target.setFranchiseName(source.getFranchiseName());

        target.setVat(source.getVat());

        // channel
        target.setChannel(source.getChannel());

        // initiative
        target.setInitiativeId(source.getInitiativeId());
        target.setInitiativeName(source.getInitiativeName());

        target.setInitiatives(source.getInitiatives());

        // user
        target.setUserId(source.getUserId());

        // acquirer
        target.setAcquirerId(source.getAcquirerId());

        // pos
        target.setPointOfSaleId(source.getPointOfSaleId());
        target.setPointOfSaleType(source.getPointOfSaleType());

        // family
        target.setFamilyId(source.getFamilyId());

        // rewards
        target.setRewards(source.getRewards());

        // rejection reasons
        target.setRejectionReasons(
                source.getRejectionReasons());

        target.setInitiativeRejectionReasons(
                source.getInitiativeRejectionReasons());

        // custom properties
        target.setAdditionalProperties(
                source.getAdditionalProperties());

        // versioning
        target.setCounterVersion(
                source.getCounterVersion());

        // misc
        target.setMcc(source.getMcc());

        target.setExtendedAuthorization(
                source.getExtendedAuthorization());

        target.setInvoiceData(source.getInvoiceData());
        target.setCreditNoteData(source.getCreditNoteData());
    }

}