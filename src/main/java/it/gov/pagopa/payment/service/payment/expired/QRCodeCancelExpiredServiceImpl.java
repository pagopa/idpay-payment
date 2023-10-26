package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends BaseCommonCodeExpiration implements QRCodeCancelExpiredService {

    private final long cancelExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonConfirmServiceImpl commonConfirmService;

    public QRCodeCancelExpiredServiceImpl(
            @Value("${app.qrCode.expirations.cancelMinutes:15}") long cancelExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            AuditUtilities auditUtilities,
            CommonConfirmServiceImpl commonConfirmService) {
        super(auditUtilities, RewardConstants.TRX_CHANNEL_QRCODE);

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.cancelExpirationMinutes = cancelExpirationMinutes;
        this.commonConfirmService = commonConfirmService;
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
        commonConfirmService.confirmAuthorizedPayment(trx);
        return trx;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_CANCEL_EXPIRED";
    }
}
