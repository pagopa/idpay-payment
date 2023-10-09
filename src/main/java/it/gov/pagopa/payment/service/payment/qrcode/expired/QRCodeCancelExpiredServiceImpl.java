package it.gov.pagopa.payment.service.payment.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends BaseCommonCodeExpiration implements QRCodeCancelExpiredService {

    private final long cancelExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final QRCodeConfirmationService qrCodeConfirmationService;
    private static String expiredCode = "EXPIRED_QR_CODE";

    public QRCodeCancelExpiredServiceImpl(
            @Value("${app.qrCode.expirations.cancelMinutes:15}") long cancelExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            QRCodeConfirmationService qrCodeConfirmationService,
            AuditUtilities auditUtilities
            ) {
        super(auditUtilities, expiredCode);

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.qrCodeConfirmationService = qrCodeConfirmationService;

        this.cancelExpirationMinutes = cancelExpirationMinutes;
    }

    @Override
    protected long getExpirationMinutes() {
        return cancelExpirationMinutes;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes) {
        return transactionInProgressRepository.findCancelExpiredTransaction(initiativeId, expirationMinutes);
    }

    @Override
    protected TransactionInProgress handleExpiredTransaction(TransactionInProgress trx) {
        qrCodeConfirmationService.confirmAuthorizedPayment(trx);
        return trx;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_CANCEL_EXPIRED";
    }
}
