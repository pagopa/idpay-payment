package it.gov.pagopa.payment.service.payment.idpaycode.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonAuthCodeExpiration;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class IdpayCodeAuthorizationExpiredServiceImpl extends CommonAuthCodeExpiration implements IdpayCodeAuthorizationExpiredService {

    public IdpayCodeAuthorizationExpiredServiceImpl(@Value("${app.common.expirations.authorizationMinutes:5}") long authorizationExpirationMinutes,
                                                    TransactionRepository transactionRepository,
                                                    AuditUtilities auditUtilities,
                                                    RewardCalculatorConnector rewardCalculatorConnector) {
        super(
                auditUtilities,
                RewardConstants.TRX_CHANNEL_IDPAYCODE,
                authorizationExpirationMinutes,
                transactionRepository,
                rewardCalculatorConnector);

    }

    @Override
    public Transaction findByTrxIdAndAuthorizationNotExpired(String trxId) {
        LocalDateTime minTrxDate = LocalDateTime.now(ZoneId.of("Europe/Rome")).minusMinutes(authorizationExpirationMinutes);
        return transactionRepository.findByIdAndTrxDateGreaterThanEqual(trxId,minTrxDate)
                            .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with trxId [%s]".formatted(trxId)));
    }

}
