package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static it.gov.pagopa.payment.enums.SyncTrxStatus.IDENTIFIED;

public class CommonAuthCodeExpiration extends BaseCommonCodeExpiration{
    protected final long authorizationExpirationMinutes;
    protected final TransactionRepository transactionRepository;
    protected final RewardCalculatorConnector rewardCalculatorConnector;
    protected CommonAuthCodeExpiration(AuditUtilities auditUtilities,
                                       String channel,
                                       long authorizationExpirationMinutes,
                                       TransactionRepository transactionRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector) {
        super(auditUtilities, channel);
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionRepository = transactionRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
    }

    @Override
    protected long getExpirationMinutes() {
        return authorizationExpirationMinutes;
    }

    @Override
    protected Transaction findExpiredTransaction(String initiativeId, long expirationMinutes) {
        return transactionRepository.findAuthorizationExpiredTransaction(
                initiativeId,
                LocalDateTime.now(ZoneId.of("Europe/Rome")).minusMinutes(authorizationExpirationMinutes),
                List.of("IDENTIFIED", "CREATED", "REJECTED"),
                1000
        );
    }

    @Override
    protected Transaction handleExpiredTransaction(Transaction transaction) {
        if (transaction.getStatus().equals(IDENTIFIED)) {
            try {
                rewardCalculatorConnector.cancelTransaction(transaction);
            } catch (ServiceException e) {
                if (! (e instanceof TransactionNotFoundOrExpiredException)) {
                    throw new InternalServerErrorException(PaymentConstants.ExceptionCode.GENERIC_ERROR,
                            "An error occurred in the microservice reward-calculator while handling transaction with id %s".formatted(transaction.getId()),true,e);
                }
            }
        }

        transaction.setStatus(SyncTrxStatus.EXPIRED);
        transactionRepository.save(transaction);
        return transaction;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_AUTHORIZATION_EXPIRED";
    }
}
