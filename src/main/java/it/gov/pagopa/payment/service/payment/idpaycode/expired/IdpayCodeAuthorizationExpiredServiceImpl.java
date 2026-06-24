package it.gov.pagopa.payment.service.payment.idpaycode.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonAuthCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class IdpayCodeAuthorizationExpiredServiceImpl extends CommonAuthCodeExpiration implements IdpayCodeAuthorizationExpiredService {

    public IdpayCodeAuthorizationExpiredServiceImpl(@Value("${app.common.expirations.authorizationMinutes:5}") long authorizationExpirationMinutes,
                                                    TransactionRepository transactionRepository,
                                                    TransactionInProgressRepository transactionInProgressRepository,
                                                    AuditUtilities auditUtilities,
                                                    RewardCalculatorConnector rewardCalculatorConnector) {
        super(
                auditUtilities,
                RewardConstants.TRX_CHANNEL_IDPAYCODE,
                authorizationExpirationMinutes,
                transactionRepository,
                transactionInProgressRepository,
                rewardCalculatorConnector);

    }

    @Override
    public TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String trxId) {
        OffsetDateTime minTrxDate = OffsetDateTime.now().minusMinutes(authorizationExpirationMinutes);
        transactionRepository.findByTrxIdAndAuthorizationNotExpired(trxId,minTrxDate)
                            .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with trxId [%s]".formatted(trxId)));
        return transactionInProgressRepository.findByTrxIdAndAuthorizationNotExpired(trxId,authorizationExpirationMinutes);
    }

}
