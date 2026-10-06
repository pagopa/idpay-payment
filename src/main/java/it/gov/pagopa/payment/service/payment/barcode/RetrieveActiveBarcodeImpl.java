package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static it.gov.pagopa.payment.utils.RewardConstants.TRX_CHANNEL_BARCODE;

@Slf4j
@Service
public class RetrieveActiveBarcodeImpl implements RetrieveActiveBarcode{
    public static final String NO_ACTIVE_TRANSACTION_FOUND_FOR_USER = "No active transaction found for user";
    private static final List<SyncTrxStatus> ACTIVE_TRX_STATUSES = List.of(SyncTrxStatus.AUTHORIZED, SyncTrxStatus.CAPTURED, SyncTrxStatus.INVOICED, SyncTrxStatus.REWARDED);
    private final Map<String, Optional<Long>> initiativeMaxTrxAllowedCache = new ConcurrentHashMap<>();
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final RewardRuleRepository rewardRuleRepository;

    public RetrieveActiveBarcodeImpl(TransactionRepository transactionRepository, TransactionMapper transactionMapper, RewardRuleRepository rewardRuleRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.rewardRuleRepository = rewardRuleRepository;
    }

    @Override
    public TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndInitiativeIdAndChannel(userId, initiativeId, TRX_CHANNEL_BARCODE);

        Long initiativeMaxTrxNumber = getMaxTrxNumberForInitiative(initiativeId);

        Transaction latest = null;
        long usedCounter = 0L;

        for (Transaction trx : transactions) {
            SyncTrxStatus status = trx.getStatus();

            if (initiativeMaxTrxNumber != null
                    && ACTIVE_TRX_STATUSES.contains(status)
                    && ++usedCounter >= initiativeMaxTrxNumber) {
                throw new TransactionAlreadyAuthorizedException("The maximum number of transaction authorizations (%d) has been reached".formatted(initiativeMaxTrxNumber));

            }

            if (status == SyncTrxStatus.CREATED
                    && (latest == null || trx.getTrxDate().isBefore(latest.getTrxDate()))) {
                latest = trx;
            }
        }

        if(null != latest) {
            latest.setAmountCents(latest.getVoucherAmountCents());
        } else {
            throw new TransactionNotFoundOrExpiredException(NO_ACTIVE_TRANSACTION_FOUND_FOR_USER);
        }

        return transactionMapper.transactionBarCodeToTransactionResponse(latest);
    }

    private Long getMaxTrxNumberForInitiative(String initiativeId) {
        return initiativeMaxTrxAllowedCache.computeIfAbsent(initiativeId, id -> {
            InitiativeConfig initiative = rewardRuleRepository.findById(id)
                    .map(RewardRule::getInitiativeConfig)
                    .orElseThrow(() -> new InitiativeNotfoundException("Cannot find initiative with id [%s]".formatted(Utilities.sanitizeString(id))));

            Long maxTrx = null;
            if (initiative.getTrxRule() != null
                    && initiative.getTrxRule().getTrxCount() != null
                    && initiative.getTrxRule().getTrxCount().getTo() != null) {

                long to = initiative.getTrxRule().getTrxCount().getTo();
                if (initiative.getTrxRule().getTrxCount().isToIncluded()) {
                    maxTrx = to;
                } else {
                    maxTrx = Math.max(0L, to - 1);
                }
            }

            return Optional.ofNullable(maxTrx);
        }).orElse(null);

    }


}