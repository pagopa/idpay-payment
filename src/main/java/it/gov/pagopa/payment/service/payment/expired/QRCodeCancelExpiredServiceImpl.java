package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends BaseCommonCodeExpiration implements QRCodeCancelExpiredService {

    private final long cancelExpirationMinutes;

    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonConfirmServiceImpl commonConfirmService;

    public QRCodeCancelExpiredServiceImpl(
            @Value("${app.qrCode.expirations.cancelMinutes:15}") long cancelExpirationMinutes,
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            AuditUtilities auditUtilities,
            CommonConfirmServiceImpl commonConfirmService) {
        super(auditUtilities, RewardConstants.TRX_CHANNEL_QRCODE);
        this.transactionRepository = transactionRepository;
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
        OffsetDateTime maxTrxDate = OffsetDateTime.now().minusMinutes(cancelExpirationMinutes);
        List<String> statusList = List.of(SyncTrxStatus.AUTHORIZED.name());
        transactionRepository.findAndModifyExpiredTransaction(
                maxTrxDate,
                statusList,
                initiativeId,
                1000
        );
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
