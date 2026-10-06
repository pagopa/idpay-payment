package it.gov.pagopa.payment.service.payment.barcode.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.expired.common.CommonAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BarCodeAuthorizationExpiredServiceImpl extends CommonAuthorizationExpiredServiceImpl implements BarCodeAuthorizationExpiredService {

    public BarCodeAuthorizationExpiredServiceImpl(
            TransactionRepository transactionRepository,
            @Value("${app.bar-code.expirations.authorization-minutes}") long authorizationExpirationMinutes,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        super(transactionRepository,
                authorizationExpirationMinutes,
                rewardCalculatorConnector,
                auditUtilities,
                RewardConstants.TRX_CHANNEL_BARCODE);
    }
}
