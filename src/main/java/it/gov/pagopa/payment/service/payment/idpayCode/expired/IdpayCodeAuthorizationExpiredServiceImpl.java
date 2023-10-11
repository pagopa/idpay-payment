package it.gov.pagopa.payment.service.payment.idpayCode.expired;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.qrcode.expired.BaseCommonCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdpayCodeAuthorizationExpiredServiceImpl extends BaseCommonCodeExpiration implements IdpayCodeAuthorizationExpiredService {
    private final long authorizationExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;

    public IdpayCodeAuthorizationExpiredServiceImpl(@Value("${app.idpayCode.expirations.authorizationMinutes:5}") long authorizationExpirationMinutes,
                                                    TransactionInProgressRepository transactionInProgressRepository,
                                                    AuditUtilities auditUtilities,
                                                    RewardCalculatorConnector rewardCalculatorConnector) {
        super(auditUtilities, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
    }

    @Override
    public TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String trxId) {
        return transactionInProgressRepository.findByTrxIdAndAuthorizationNotExpired(trxId,authorizationExpirationMinutes);
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
