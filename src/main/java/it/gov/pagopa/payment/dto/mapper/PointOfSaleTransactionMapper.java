package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.service.PDVService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PointOfSaleTransactionMapper {

    private final PDVService pdvService;

    public PointOfSaleTransactionMapper(PDVService pdvService) {
        this.pdvService = pdvService;
    }

    public PointOfSaleTransactionDTO toPointOfSaleTransactionDTO(Transaction trx, String fiscalCodeInput) {
        Long totalAmount = trx.getAmountCents();

        Long rewardAmount = 0L;

        if (trx.getRewards() != null && trx.getRewards().get(trx.getInitiativeId()) != null) {
            rewardAmount = Math.abs(trx.getRewards().get(trx.getInitiativeId()).getAccruedRewardCents());
        }

        Long authorizedAmount = totalAmount - rewardAmount;

        InvoiceData invoiceFile = null;

        if ((SyncTrxStatus.INVOICED.equals(trx.getStatus())
                || SyncTrxStatus.REWARDED.equals(trx.getStatus()))
                && trx.getInvoiceData() != null) {
            invoiceFile = InvoiceData.builder()
                    .filename(trx.getInvoiceData().getFilename())
                    .docNumber(trx.getInvoiceData().getDocNumber())
                    .build();
        } else if (SyncTrxStatus.REFUNDED.equals(trx.getStatus())
                && trx.getCreditNoteData() != null) {
            invoiceFile = InvoiceData.builder()
                    .filename(trx.getCreditNoteData().getFilename())
                    .docNumber(trx.getCreditNoteData().getDocNumber())
                    .build();
        }

        String fiscalCode = resolveFiscalCode(fiscalCodeInput, trx.getUserId());

        return PointOfSaleTransactionDTO.builder()
                .trxId(trx.getId())
                .trxCode(trx.getTrxCode())
                .effectiveAmountCents(trx.getAmountCents())
                .rewardAmountCents(rewardAmount)
                .authorizedAmountCents(authorizedAmount)
                .trxDate(trx.getTrxDate())
                .trxChargeDate(trx.getTrxChargeDate())
                .elaborationDateTime(trx.getElaborationDateTime() != null ? trx.getElaborationDateTime() : null)
                .status(String.valueOf(trx.getStatus()))
                .rewardBatchTrxStatus(trx.getRewardBatchStatusTrx() != null ? trx.getRewardBatchStatusTrx() : null)
                .channel(trx.getChannel())
                .fiscalCode(fiscalCode)
                .additionalProperties(trx.getAdditionalProperties())
                .invoiceData(invoiceFile)
                .build();
    }

    private String resolveFiscalCode(String fiscalCodeInput, String userId) {
        if (StringUtils.isNotBlank(fiscalCodeInput)) {
            return fiscalCodeInput;
        }
        return userId != null ? pdvService.decryptCF(userId) : null;
    }

}
