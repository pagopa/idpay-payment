package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends BaseQRCodeExpiration implements QRCodeCancelExpiredService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final QRCodeConfirmationService qrCodeConfirmationService;

    public QRCodeCancelExpiredServiceImpl(TransactionInProgressRepository transactionInProgressRepository, QRCodeConfirmationService qrCodeConfirmationService) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.qrCodeConfirmationService = qrCodeConfirmationService;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction() {
        return transactionInProgressRepository.findCancelExpiredTransactionThrottled();
    }

    @Override
    @PerformanceLog(EXPIRED_QR_CODE)
    protected void handleExpiredTransaction(TransactionInProgress trx) {
        qrCodeConfirmationService.confirmAuthorizedPayment(trx);
        auditUtilities.logExpiredTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), getFlowName());
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_CANCEL_EXPIRED";
    }
}
