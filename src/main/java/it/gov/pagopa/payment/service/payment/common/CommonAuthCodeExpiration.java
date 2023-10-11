package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.springframework.http.HttpStatus;

public class CommonAuthCodeExpiration extends BaseCommonCodeExpiration{
    protected final long authorizationExpirationMinutes;
    protected final TransactionInProgressRepository transactionInProgressRepository;
    protected final RewardCalculatorConnector rewardCalculatorConnector;
    protected CommonAuthCodeExpiration(AuditUtilities auditUtilities,
                                       String channel,
                                       long authorizationExpirationMinutes,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector) {
        super(auditUtilities, channel);
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
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
        if (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) { //TODO BRCODE controllare stato CREATED
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
