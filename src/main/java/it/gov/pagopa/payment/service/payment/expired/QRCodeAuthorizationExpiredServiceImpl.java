package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.expired.common.CommonAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthorizationExpiredServiceImpl extends CommonAuthorizationExpiredServiceImpl implements QRCodeAuthorizationExpiredService {

    public QRCodeAuthorizationExpiredServiceImpl(
            @Value("${app.common.expirations.authorizationMinutes}") long authorizationExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        super(
                authorizationExpirationMinutes,
                transactionInProgressRepository,
                rewardCalculatorConnector,
                auditUtilities,
                RewardConstants.TRX_CHANNEL_QRCODE);
    }

}
