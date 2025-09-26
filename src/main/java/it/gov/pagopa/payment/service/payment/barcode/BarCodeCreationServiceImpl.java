package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionInProgressService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl.checkInitiativeType;
import static it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl.checkInitiativeValidPeriod;

@Slf4j
@Service
public class BarCodeCreationServiceImpl implements BarCodeCreationService {

    private static final String BAR_CODE_CREATE_TRANSACTION = "BAR_CODE_CREATE_TRANSACTION";
    private final TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;
    private final RewardRuleRepository rewardRuleRepository;
    private final WalletConnector walletConnector;
    private final AuditUtilities auditUtilities;
    private final TransactionInProgressService transactionInProgressService;

    private final int authorizationExpirationMinutes;
    private final int extendedAuthorizationExpirationMinutes;


    protected BarCodeCreationServiceImpl(RewardRuleRepository rewardRuleRepository,
                                         AuditUtilities auditUtilities,
                                         TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper,
                                         TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper,
                                         WalletConnector walletConnector,
                                         TransactionInProgressService transactionInProgressService,
                                         @Value("${app.barCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes,
                                         @Value("${app.barCode.expirations.extendedAuthorizationMinutes}") int extendedAuthorizationExpirationMinutes
                                         ) {

        this.transactionBarCodeCreationRequest2TransactionInProgressMapper = transactionBarCodeCreationRequest2TransactionInProgressMapper;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
        this.walletConnector = walletConnector;
        this.rewardRuleRepository = rewardRuleRepository;
        this.auditUtilities = auditUtilities;
        this.transactionInProgressService = transactionInProgressService;
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.extendedAuthorizationExpirationMinutes = extendedAuthorizationExpirationMinutes;
    }

    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                        String channel,
                                                        String userId) {

        LocalDate today = LocalDate.now();

        try {
            InitiativeConfig initiative = checkInitiative(trxBarCodeCreationRequest, today);

            Long residualBudgetCents = checkWallet(trxBarCodeCreationRequest.getInitiativeId(), userId);

            TransactionInProgress trx = generateAndSaveTransaction(trxBarCodeCreationRequest, channel, userId, false, initiative);

            trx.setAmountCents(residualBudgetCents);
            return transactionBarCodeInProgress2TransactionResponseMapper.apply(trx);

        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }

    @Override
    public TransactionBarCodeResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                                        String channel,
                                                                        String userId) {

        LocalDate today = LocalDate.now();

        try {
            InitiativeConfig initiative = checkInitiative(trxBarCodeCreationRequest, today);
            checkVoucherAmountCents(trxBarCodeCreationRequest.getInitiativeId(), trxBarCodeCreationRequest.getVoucherAmountCents());
            TransactionInProgress trx = generateAndSaveTransaction(trxBarCodeCreationRequest, channel, userId, true, initiative);
            return transactionBarCodeInProgress2TransactionResponseMapper.apply(trx);

        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }


    @NotNull
    private TransactionInProgress generateAndSaveTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String channel, String userId, boolean extendedAuthorization, InitiativeConfig initiative) {
        OffsetDateTime trxEndDate = null;
        TransactionInProgress trx =
                transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                        trxBarCodeCreationRequest, channel, userId, initiative != null ? initiative.getInitiativeName() : null, new HashMap<>(), extendedAuthorization, trxEndDate);

        trxEndDate = calculateTrxEndDate(trx, initiative);
        trx.setTrxEndDate(trxEndDate);
        transactionInProgressService.generateTrxCodeAndSave(trx, getFlow());

        logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), userId);
        return trx;
    }

    public OffsetDateTime calculateTrxEndDate(TransactionInProgress transactionInProgress,  InitiativeConfig initiative) {
        if (Boolean.FALSE.equals(transactionInProgress.getExtendedAuthorization())){
            return transactionInProgress.getTrxDate().plusMinutes(authorizationExpirationMinutes);

        }

        LocalDate  localEndDate = LocalDate.MAX;
        if (initiative != null){
            localEndDate = initiative.getEndDate() != null ? initiative.getEndDate() : LocalDate.MAX;
        }
        OffsetDateTime  offsetEndDate = localEndDate.atStartOfDay().atOffset(ZoneOffset.of("+02:00"));
        if(!(offsetEndDate.minusMinutes(extendedAuthorizationExpirationMinutes).isBefore(transactionInProgress.getTrxDate()))){
            offsetEndDate = transactionInProgress.getTrxDate().plusMinutes(extendedAuthorizationExpirationMinutes);
        }
        return offsetEndDate.truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);
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

        return wallet.getAmountCents();
    }

    private void checkVoucherAmountCents(String initiativeId, Long voucherAmountCents){
        if (voucherAmountCents < 0L) {
            throw new BudgetExhaustedException(String.format("Budget exhausted for the current user and initiative [%s]", initiativeId));
        }
    }
}
