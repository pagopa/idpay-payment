package it.gov.pagopa.payment.service.payment.qrcode.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonAuthCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthorizationExpiredServiceImpl extends CommonAuthCodeExpiration implements QRCodeAuthorizationExpiredService {

    private final long authorizationExpirationMinutes;

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    public QRCodeAuthorizationExpiredServiceImpl(
            @Value("${app.qrCode.expirations.authorizationMinutes:15}") long authorizationExpirationMinutes,

            TransactionInProgressRepository transactionInProgressRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            AuditUtilities auditUtilities) {
        super(auditUtilities, RewardConstants.TRX_CHANNEL_QRCODE,authorizationExpirationMinutes,transactionInProgressRepository,rewardCalculatorConnector);

        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode) {
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired(trxCode, authorizationExpirationMinutes);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode) {
        return transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(trxCode, authorizationExpirationMinutes);
    }

}
