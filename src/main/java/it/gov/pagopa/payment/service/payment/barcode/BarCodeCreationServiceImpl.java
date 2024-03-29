package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.common.utils.CommonUtilities.euroToCents;

@Slf4j
@Service
public class BarCodeCreationServiceImpl extends CommonCreationServiceImpl implements BarCodeCreationService {

    private final TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;
    private final WalletConnector walletConnector;
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    protected BarCodeCreationServiceImpl(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
                                         TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper,
                                         RewardRuleRepository rewardRuleRepository,
                                         TransactionInProgressRepository transactionInProgressRepository,
                                         TrxCodeGenUtil trxCodeGenUtil,
                                         AuditUtilities auditUtilities,
                                         MerchantConnector merchantConnector,
                                         TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper,
                                         TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper, WalletConnector walletConnector) {
        super(transactionInProgress2TransactionResponseMapper,
                transactionCreationRequest2TransactionInProgressMapper,
                rewardRuleRepository,
                transactionInProgressRepository,
                trxCodeGenUtil,
                auditUtilities,
                merchantConnector);
        this.transactionBarCodeCreationRequest2TransactionInProgressMapper = transactionBarCodeCreationRequest2TransactionInProgressMapper;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
        this.walletConnector = walletConnector;
    }
    @Override
    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
                                                        String channel,
                                                        String userId) {

        LocalDate today = LocalDate.now();

        try {
            InitiativeConfig initiative = rewardRuleRepository.findById(trxBarCodeCreationRequest.getInitiativeId())
                    .map(RewardRule::getInitiativeConfig)
                    .orElse(null);

            checkInitiativeType(trxBarCodeCreationRequest.getInitiativeId(), initiative);

            checkInitiativeValidPeriod(today, initiative);

            Long residualBudgetCents = checkWallet(trxBarCodeCreationRequest.getInitiativeId(), userId);

            TransactionInProgress trx =
                    transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                            trxBarCodeCreationRequest, channel, userId, initiative != null ? initiative.getInitiativeName() : null);
            generateTrxCodeAndSave(trx);

            logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), userId);

            trx.setAmountCents(residualBudgetCents);
            return transactionBarCodeInProgress2TransactionResponseMapper.apply(trx);

        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }

    @Override
    protected void logCreatedTransaction(String initiativeId, String id, String trxCode, String userId) {
        auditUtilities.logBarCodeCreatedTransaction(initiativeId, id, trxCode, userId);
    }

    @Override
    protected  void logErrorCreatedTransaction(String initiativeId,String userId){
        auditUtilities.logBarCodeErrorCreatedTransaction(initiativeId,userId);
    }

    @Override
    public String getFlow(){
        return "BAR_CODE_CREATE_TRANSACTION";
    }

    private Long checkWallet(String initiativeId, String userId){
        WalletDTO wallet = walletConnector.getWallet(initiativeId, userId);

        if (wallet.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BudgetExhaustedException(String.format("Budget exhausted for the current user and initiative [%s]", initiativeId));
        }

        if (PaymentConstants.WALLET_STATUS_UNSUBSCRIBED.equals(wallet.getStatus())){
            throw new UserNotOnboardedException(ExceptionCode.USER_UNSUBSCRIBED, "The user has unsubscribed from initiative [%s]".formatted(initiativeId));
        }

        return euroToCents(wallet.getAmount());
    }
}
