package it.gov.pagopa.payment.service.payment.qrcode.expired;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthorizationExpiredServiceImpl extends BaseQRCodeExpiration implements QRCodeAuthorizationExpiredService {

    private final long authorizationExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;

    public QRCodeAuthorizationExpiredServiceImpl(
            @Value("${app.qrCode.expirations.authorizationMinutes:15}") long authorizationExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        super(auditUtilities);

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;

        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode) {
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode, authorizationExpirationMinutes);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode) {
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(trxCode, authorizationExpirationMinutes);
    }

    @Override
    protected long getExpirationMinutes() {
        return authorizationExpirationMinutes;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes) {
        return transactionInProgressRepository.findAuthorizationExpiredTransaction(initiativeId, expirationMinutes);
    }

    @Override
    protected TransactionInProgress handleExpiredTransaction(TransactionInProgress trx) {
        if (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
            try {
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (ClientException e) {
                if (e.getHttpStatus() != HttpStatus.NOT_FOUND) {
                    throw new IllegalStateException("An error occurred in the microservice reward-calculator while handling transaction with id %s".formatted(trx.getId()), e);
                }
            }
        }
        transactionInProgressRepository.deleteById(trx.getId());
        return trx;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_AUTHORIZATION_EXPIRED";
    }

}
