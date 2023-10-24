package it.gov.pagopa.payment.service.payment.expired.common;

import it.gov.pagopa.common.web.exception.custom.ServiceException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public abstract class CommonAuthorizationExpiredServiceImpl extends BaseCommonCodeExpiration {

    private final long authorizationExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;

    protected CommonAuthorizationExpiredServiceImpl(
            long authorizationExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities,
            String channel) {
        super(auditUtilities, channel);

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;

        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    }

    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode) {
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode, authorizationExpirationMinutes);
    }

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
            } catch (ServiceException e) {
                if (! (e instanceof TransactionNotFoundOrExpiredException)) {
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
