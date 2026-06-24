package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;

import java.time.OffsetDateTime;
import java.util.List;

public class CommonAuthCodeExpiration extends BaseCommonCodeExpiration{
    protected final long authorizationExpirationMinutes;
    protected final TransactionInProgressRepository transactionInProgressRepository;
    protected final TransactionRepository transactionRepository;
    protected final RewardCalculatorConnector rewardCalculatorConnector;
    protected CommonAuthCodeExpiration(AuditUtilities auditUtilities,
                                       String channel,
                                       long authorizationExpirationMinutes,
                                       TransactionRepository transactionRepository,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector) {
        super(auditUtilities, channel);
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
    }

    @Override
    protected long getExpirationMinutes() {
        return authorizationExpirationMinutes;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes) {
        transactionRepository.findAuthorizationExpiredTransaction(
                initiativeId,
                OffsetDateTime.now().minusMinutes(authorizationExpirationMinutes),
                List.of("IDENTIFIED", "CREATED", "REJECTED"),
                1000
        );
        return transactionInProgressRepository.findAuthorizationExpiredTransaction(initiativeId, expirationMinutes);
    }

    @Override
    protected TransactionInProgress handleExpiredTransaction(TransactionInProgress trx) {
        if (trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)) {
            try {
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (ServiceException e) {
                if (! (e instanceof TransactionNotFoundOrExpiredException)) {
                    throw new InternalServerErrorException(PaymentConstants.ExceptionCode.GENERIC_ERROR,
                            "An error occurred in the microservice reward-calculator while handling transaction with id %s".formatted(trx.getId()),true,e);
                }
            }
        }
        transactionRepository.deleteById(trx.getId());
        transactionInProgressRepository.deleteById(trx.getId());
        return trx;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_AUTHORIZATION_EXPIRED";
    }
}
