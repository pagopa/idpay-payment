package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImplQRCodeExpiration extends BaseQRCodeExpiration implements QRCodeCancelExpiredService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final QRCodeConfirmationService qrCodeConfirmationService;
    private final AuditUtilities auditUtilities;

    public QRCodeCancelExpiredServiceImplQRCodeExpiration(TransactionInProgressRepository transactionInProgressRepository, QRCodeConfirmationService qrCodeConfirmationService, AuditUtilities auditUtilities) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.qrCodeConfirmationService = qrCodeConfirmationService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction() {
        return transactionInProgressRepository.findCancelExpiredTransaction();
    }

    @Override
    protected void handleExpiredTransaction(TransactionInProgress trx) {
        qrCodeConfirmationService.confirmAuthorizedPayment(trx);
        auditUtilities.logExpiredTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), getFlowName());
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_CANCEL_EXPIRED";
    }
}
