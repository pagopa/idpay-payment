package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

            checkWallet(trxBarCodeCreationRequest.getInitiativeId(), userId);

            TransactionInProgress trx =
                    transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                            trxBarCodeCreationRequest, channel, userId);
            generateTrxCodeAndSave(trx);

            logCreatedTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), userId);

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

    private void checkWallet(String initiativeId, String userId){
        BigDecimal walletAmount = walletConnector.getWallet(initiativeId, userId).getAmount();

        if (walletAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN,
                    PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED,
                    String.format("The budget related to the user on initiativeId [%s] was exhausted.", initiativeId));
        }
    }
}
