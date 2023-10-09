package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
@Slf4j
@Service
public class BarCodeCreationServiceImpl extends CommonCreationServiceImpl implements BarCodeCreationService {

    private final TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    private final TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;
    protected BarCodeCreationServiceImpl(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
                                         TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper,
                                         RewardRuleRepository rewardRuleRepository,
                                         TransactionInProgressRepository transactionInProgressRepository,
                                         TrxCodeGenUtil trxCodeGenUtil,
                                         AuditUtilities auditUtilities,
                                         MerchantConnector merchantConnector,
                                         TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper,
                                         TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper) {
        super(transactionInProgress2TransactionResponseMapper,
                transactionCreationRequest2TransactionInProgressMapper,
                rewardRuleRepository,
                transactionInProgressRepository,
                trxCodeGenUtil,
                auditUtilities,
                merchantConnector);
        this.transactionBarCodeCreationRequest2TransactionInProgressMapper = transactionBarCodeCreationRequest2TransactionInProgressMapper;
        this.transactionBarCodeInProgress2TransactionResponseMapper = transactionBarCodeInProgress2TransactionResponseMapper;
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
