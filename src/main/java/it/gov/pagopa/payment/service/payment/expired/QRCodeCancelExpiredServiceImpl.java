package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
public class QRCodeCancelExpiredServiceImpl extends BaseCommonCodeExpiration implements QRCodeCancelExpiredService {

    private final long cancelExpirationMinutes;

    private final TransactionRepository transactionRepository;
    private final CommonConfirmServiceImpl commonConfirmService;

    public QRCodeCancelExpiredServiceImpl(
            @Value("${app.qrCode.expirations.cancelMinutes:15}") long cancelExpirationMinutes,
            TransactionRepository transactionRepository,
            AuditUtilities auditUtilities,
            CommonConfirmServiceImpl commonConfirmService) {
        super(auditUtilities, RewardConstants.TRX_CHANNEL_QRCODE);
        this.transactionRepository = transactionRepository;
        this.cancelExpirationMinutes = cancelExpirationMinutes;
        this.commonConfirmService = commonConfirmService;
    }

    @Override
    protected long getExpirationMinutes() {
        return cancelExpirationMinutes;
    }

    @Override
    protected Transaction findExpiredTransaction(String initiativeId, long expirationMinutes) {
        LocalDateTime maxTrxDate = LocalDateTime.now(ZoneId.of("Europe/Rome")).minusMinutes(cancelExpirationMinutes);
        List<String> statusList = List.of(SyncTrxStatus.AUTHORIZED.name());
        return transactionRepository.findAndModifyExpiredTransaction(
                maxTrxDate,
                statusList,
                initiativeId,
                1000
        )                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                "Cannot find transaction in findExpiredTransaction with initiativeId [%s]".formatted(initiativeId)));
    }

    @Override
    protected Transaction handleExpiredTransaction(Transaction transaction) {
        commonConfirmService.confirmAuthorizedPayment(transaction);
        return transaction;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_CANCEL_EXPIRED";
    }
}
