package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.expired.common.CommonAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BarCodeAuthorizationExpiredServiceImpl extends CommonAuthorizationExpiredServiceImpl implements BarCodeAuthorizationExpiredService {

    public BarCodeAuthorizationExpiredServiceImpl(
            @Value("${app.barCode.expirations.authorizationMinutes:5}") long authorizationExpirationMinutes,
            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities);
    }
}
