package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;

import static it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl.checkInitiativeType;
import static it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl.checkInitiativeValidPeriod;

@Slf4j
@Service
public class BarCodeCreationServiceImpl implements BarCodeCreationService {

    private static final String ZONE_EUROPE_ROME = "Europe/Rome";
    private static final String BAR_CODE_CREATE_TRANSACTION = "BAR_CODE_CREATE_TRANSACTION";
    private final TransactionMapper transactionMapper;
    private final RewardRuleRepository rewardRuleRepository;
    private final WalletConnector walletConnector;
    private final AuditUtilities auditUtilities;
    private final TransactionService transactionService;

    private final int authorizationExpirationMinutes;
    private final int extendedAuthorizationExpirationMinutes;


    protected BarCodeCreationServiceImpl(RewardRuleRepository rewardRuleRepository,
                                         AuditUtilities auditUtilities,
                                         TransactionMapper transactionMapper,
                                         WalletConnector walletConnector,
                                         TransactionService transactionService,
                                         @Value("${app.bar-code.expirations.authorization-minutes}") int authorizationExpirationMinutes,
                                         @Value("${app.bar-code.expirations.extended-authorization-minutes}") int extendedAuthorizationExpirationMinutes
    ) {
        this.transactionMapper = transactionMapper;
        this.walletConnector = walletConnector;
        this.rewardRuleRepository = rewardRuleRepository;
        this.auditUtilities = auditUtilities;
        this.transactionService = transactionService;
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.extendedAuthorizationExpirationMinutes = extendedAuthorizationExpirationMinutes;
    }

    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                        String channel,
                                                        String userId) {

        LocalDate today = LocalDate.now(ZoneId.of(ZONE_EUROPE_ROME));

        try {
            InitiativeConfig initiative = checkInitiative(trxBarCodeCreationRequest, today);

            Long residualBudgetCents = checkWallet(trxBarCodeCreationRequest.getInitiativeId(), userId);

            trxBarCodeCreationRequest.setVoucherAmountCents(residualBudgetCents);
            Transaction transaction = generateAndSaveTransaction(trxBarCodeCreationRequest, channel, userId, false, initiative);
            transaction.setAmountCents(residualBudgetCents);

            return transactionMapper.transactionToTransactionBarCodeResponse(transaction);
        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }

    @Override
    public TransactionBarCodeResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                                String channel,
                                                                String userId) {

        LocalDate today = LocalDate.now(ZoneId.of(ZONE_EUROPE_ROME));

        try {
            InitiativeConfig initiative = checkInitiative(trxBarCodeCreationRequest, today);
            checkVoucherAmountCents(trxBarCodeCreationRequest.getInitiativeId(), trxBarCodeCreationRequest.getVoucherAmountCents());
            trxBarCodeCreationRequest.setVoucherAmountCents(trxBarCodeCreationRequest.getVoucherAmountCents());
            Transaction transaction = generateAndSaveTransaction(trxBarCodeCreationRequest, channel, userId, true, initiative);

            return transactionMapper.transactionToTransactionBarCodeResponse(transaction);
        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }

    public Transaction createExtendedTransactionPostDelete(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                           String channel,
                                                           String userId,
                                                           OffsetDateTime trxEndDate) {

        LocalDate today = LocalDate.now(ZoneId.of(ZONE_EUROPE_ROME));

        try {
            InitiativeConfig initiative = checkInitiative(trxBarCodeCreationRequest, today);
            return transactionMapper.transactionBarCodeCreationRequestToTransaction(
                    trxBarCodeCreationRequest, channel, userId, initiative != null ? initiative.getInitiativeName() : null, new HashMap<>(), true, trxEndDate);

        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }


    @NotNull
    private Transaction generateAndSaveTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String channel, String userId, boolean extendedAuthorization, InitiativeConfig initiative) {
        OffsetDateTime trxEndDate = null;
        Transaction transaction = transactionMapper.transactionBarCodeCreationRequestToTransaction(
                trxBarCodeCreationRequest, channel, userId, initiative != null ? initiative.getInitiativeName() : null, new HashMap<>(), extendedAuthorization, trxEndDate);

        trxEndDate = calculateTrxEndDate(transaction, initiative);
        transaction.setTrxEndDate(trxEndDate);
        transactionService.generateTrxCodeAndSave(transaction, getFlow());

        logCreatedTransaction(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), userId);
        return transaction;
    }

    public OffsetDateTime calculateTrxEndDate(Transaction transaction,  InitiativeConfig initiative) {
        if (Boolean.FALSE.equals(transaction.getExtendedAuthorization())){
            return transaction.getTrxDate().plusMinutes(authorizationExpirationMinutes);
        }

        LocalDate localEndDate = LocalDate.MAX;
        if (initiative != null && initiative.getEndDate() != null){
            localEndDate = initiative.getEndDate();
        }
        OffsetDateTime offsetEndDate = localEndDate.atStartOfDay(ZoneId.of(ZONE_EUROPE_ROME)).toOffsetDateTime();
        if (!(offsetEndDate.minusMinutes(extendedAuthorizationExpirationMinutes).isBefore(transaction.getTrxDate()))) {
            offsetEndDate = transaction.getTrxDate().plusMinutes(extendedAuthorizationExpirationMinutes);
        }
        return offsetEndDate.with(LocalTime.of(23, 59, 59));
    }


    @Nullable
    private InitiativeConfig checkInitiative(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, LocalDate today) {
        InitiativeConfig initiative = rewardRuleRepository.findById(trxBarCodeCreationRequest.getInitiativeId())
                .map(RewardRule::getInitiativeConfig)
                .orElse(null);

        checkInitiativeType(trxBarCodeCreationRequest.getInitiativeId(), initiative, getFlow());

        checkInitiativeValidPeriod(today, initiative, getFlow());
        return initiative;
    }

    private void logCreatedTransaction(String initiativeId, String id, String trxCode, String userId) {
        auditUtilities.logBarCodeCreatedTransaction(initiativeId, id, trxCode, userId);
    }

    private  void logErrorCreatedTransaction(String initiativeId,String userId){
        auditUtilities.logBarCodeErrorCreatedTransaction(initiativeId,userId);
    }

    private String getFlow(){
        return BAR_CODE_CREATE_TRANSACTION;
    }

    private Long checkWallet(String initiativeId, String userId){
        WalletDTO wallet = walletConnector.getWallet(initiativeId, userId);

        if (wallet.getAmountCents() <= 0L) {
            throw new BudgetExhaustedException(String.format("Budget exhausted for the current user and initiative [%s]", initiativeId));
        }

        if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(wallet.getStatus())){
            throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(initiativeId));
        }

        return wallet.getInitialAmountCents();
    }

    private void checkVoucherAmountCents(String initiativeId, Long voucherAmountCents){
        if (voucherAmountCents != null && voucherAmountCents < 0L) {
            throw new BudgetExhaustedException(String.format("Budget exhausted for the current user and initiative [%s]", initiativeId));
        }
    }
}