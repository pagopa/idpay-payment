package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.*;
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
    protected BarCodeCreationServiceImpl(TransactionInProgress2BaseTransactionResponseMapper transactionInProgress2BaseTransactionResponseMapper,
                                         TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper,
                                         RewardRuleRepository rewardRuleRepository,
                                         TransactionInProgressRepository transactionInProgressRepository,
                                         TrxCodeGenUtil trxCodeGenUtil,
                                         AuditUtilities auditUtilities,
                                         MerchantConnector merchantConnector,
                                         TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper,
                                         TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper, WalletConnector walletConnector) {
        super(transactionInProgress2BaseTransactionResponseMapper,
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
            BigDecimal walletAmount = walletConnector.getWallet(trxBarCodeCreationRequest.getInitiativeId(), userId).getAmount();
            if (walletAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND,
                        "WALLET",
                        String.format("The budget related to the user %s with initiativeId %s was exhausted.", userId, trxBarCodeCreationRequest.getInitiativeId()));
            }
            InitiativeConfig initiative = rewardRuleRepository.findById(trxBarCodeCreationRequest.getInitiativeId())
                    .map(RewardRule::getInitiativeConfig)
                    .orElse(null);

            checkInitiativeType(trxBarCodeCreationRequest.getInitiativeId(), initiative);

            checkInitiativeValidPeriod(today, initiative);

            TransactionInProgress trx =
                    transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                            trxBarCodeCreationRequest, channel, userId);
            generateTrxCodeAndSave(trx);

            logCreatedBarCodeTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), userId);

            return transactionBarCodeInProgress2TransactionResponseMapper.apply(trx);

        } catch (RuntimeException e) {
            logErrorCreatedTransaction(trxBarCodeCreationRequest.getInitiativeId(), userId);
            throw e;
        }
    }

    protected void logCreatedBarCodeTransaction(String initiativeId, String id, String trxCode, String userId) {
        auditUtilities.logCreatedTransaction(initiativeId, id, trxCode, userId);
    }
    @Override
    public String getFlow(){
        return "BAR_CODE_CREATE_TRANSACTION";
    }
}
