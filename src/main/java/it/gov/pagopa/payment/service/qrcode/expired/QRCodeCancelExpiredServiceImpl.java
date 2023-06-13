package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends QRCodeExpirationBase implements QRCodeCancelExpiredService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final QRCodeConfirmationService qrCodeConfirmationService;

    public QRCodeCancelExpiredServiceImpl(TransactionInProgressRepository transactionInProgressRepository, QRCodeConfirmationService qrCodeConfirmationService) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.qrCodeConfirmationService = qrCodeConfirmationService;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction() {
        return transactionInProgressRepository.findCancelExpiredTransaction();
    }

    @Override
    protected void handleExpiredTransaction(TransactionInProgress trx) {
        qrCodeConfirmationService.confirmAuthorizedPayment(trx);
    }

    @Override
    protected String infoLog(TransactionInProgress trx) {
        return null; //TODO
    }
}
