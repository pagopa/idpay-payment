package it.gov.pagopa.payment.service.payment.barcode.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.expired.common.CommonAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@Slf4j
public class BarCodeAuthorizationExpiredServiceImpl extends CommonAuthorizationExpiredServiceImpl implements BarCodeAuthorizationExpiredService {

    private static final String ZONE_EUROPE_ROME = "Europe/Rome";
    private final TransactionRepository transactionRepository;

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
        this.transactionRepository = transactionRepository;
    }

    @Override
    public it.gov.pagopa.payment.entity.Transaction findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNotAndInitiativeId(String trxCode,
                                                                                                                            String initiativeId) {
        return transactionRepository
                .findByTrxCodeAndInitiativeIdAndTrxEndDateGreaterThanEqualAndStatusNot(
                        trxCode,
                        initiativeId,
                        OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)),
                        SyncTrxStatus.CANCELLED)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        CANNOT_FIND_TRANSACTION_WITH_TRX_CODE_S.formatted(trxCode.toLowerCase())));
    }
}
