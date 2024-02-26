package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements BarCodeAuthPaymentService {

    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public BarCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                         BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         RewardCalculatorConnector rewardCalculatorConnector,
                                         AuditUtilities auditUtilities,
                                         WalletConnector walletConnector,
                                         MerchantConnector merchantConnector,
                                         @Qualifier("commonPreAuth")CommonPreAuthServiceImpl commonPreAuthService,
                                         AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerService){
        super(transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector, commonPreAuthService, timeoutSchedulerService);
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String acquirerId){
        try {
            if (authBarCodePaymentDTO.getAmountCents() <= 0L) {
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid amount: [{}]", authBarCodePaymentDTO.getAmountCents());
                throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot authorize transaction with invalid amount [%s]".formatted(authBarCodePaymentDTO.getAmountCents()));
            }

            TransactionInProgress trx = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
            checkAuth(trxCode, trx);

            MerchantDetailDTO merchantDetail = merchantConnector.merchantDetail(merchantId, trx.getInitiativeId());

            checkWalletStatus(trx.getInitiativeId(), trx.getUserId());

            setTrxFields(merchantId, authBarCodePaymentDTO, trx, merchantDetail, acquirerId);

            checkTrxStatusToInvokePreAuth(trx);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, merchantId,authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
            authPaymentDTO.setResidualBudget(CommonPaymentUtilities.calculateResidualBudget(authPaymentDTO.getRewards()));
            authPaymentDTO.setRejectionReasons(Collections.emptyList());
            Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(authBarCodePaymentDTO.getAmountCents(), authPaymentDTO.getReward());
            authPaymentDTO.setSplitPayment(splitPaymentAndResidualAmountCents.getKey());
            authPaymentDTO.setResidualAmountCents(splitPaymentAndResidualAmountCents.getValue());
            return authPaymentDTO;
        } catch (RuntimeException e) {
            logErrorAuthorizedPayment(trxCode, merchantId);
            throw e;
        }
    }

    @Override
    protected void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId,Long reward, List<String> rejectionReasons){
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, reward, rejectionReasons);
    }

    @Override
    protected void logErrorAuthorizedPayment(String trxCode, String merchantId){
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    private static void setTrxFields(String merchantId, AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     TransactionInProgress trx, MerchantDetailDTO merchantDetail, String acquirerId) {
        trx.setAmountCents(authBarCodePaymentDTO.getAmountCents());
        trx.setEffectiveAmount(CommonUtilities.centsToEuro(authBarCodePaymentDTO.getAmountCents()));
        trx.setIdTrxAcquirer(authBarCodePaymentDTO.getIdTrxAcquirer());
        trx.setMerchantId(merchantId);
        trx.setBusinessName(merchantDetail.getBusinessName());
        trx.setMerchantFiscalCode(merchantDetail.getFiscalCode());
        trx.setVat(merchantDetail.getVatNumber());
        trx.setAcquirerId(acquirerId);
        trx.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
    }
}
