package it.gov.pagopa.payment.service.payment.expired.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
public abstract class CommonAuthorizationExpiredServiceImpl extends BaseCommonCodeExpiration {

    private final long authorizationExpirationMinutes;

    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;

    protected CommonAuthorizationExpiredServiceImpl(
            TransactionRepository transactionRepository,
            long authorizationExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities,
            String channel) {
        super(auditUtilities, channel);
        this.transactionRepository = transactionRepository;

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;

        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    }

    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode) {
        transactionRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode, OffsetDateTime.now(ZoneOffset.UTC));
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode);
    }

    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode) {
        OffsetDateTime minTrxDate = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(authorizationExpirationMinutes);

        transactionRepository.findAndModifyThrottled(trxCode, minTrxDate);

        if (transactionRepository.existsByTrxCodeAndDateGreaterThan(trxCode, minTrxDate)) {
            throw new TooManyRequestsException("Too many requests on trx having trCode: " + trxCode);
        }

        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(trxCode, authorizationExpirationMinutes);
    }

    @Override
    protected long getExpirationMinutes() {
        return authorizationExpirationMinutes;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes) {
        transactionRepository.findAuthorizationExpiredTransaction(
                initiativeId,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(authorizationExpirationMinutes),
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
                    throw new InternalServerErrorException(PaymentConstants.ExceptionCode.GENERIC_ERROR, "An error occurred in the microservice reward-calculator while handling transaction with id %s".formatted(trx.getId()), true, e);
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