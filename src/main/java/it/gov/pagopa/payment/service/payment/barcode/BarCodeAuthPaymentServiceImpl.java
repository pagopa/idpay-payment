package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl implements BarCodeAuthPaymentService {

    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    private final CommonAuthServiceImpl commonAuthService;
    protected final AuditUtilities auditUtilities;
    public BarCodeAuthPaymentServiceImpl(BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         MerchantConnector merchantConnector,
                                         CommonAuthServiceImpl commonAuthService,
                                         AuditUtilities auditUtilities){
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
        this.commonAuthService = commonAuthService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String acquirerId){
        try {
            if (authBarCodePaymentDTO.getAmountCents() <= 0L) {
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid amount: [{}]", authBarCodePaymentDTO.getAmountCents());
                throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot authorize transaction with invalid amount [%s]".formatted(authBarCodePaymentDTO.getAmountCents()));
            }

            TransactionInProgress trx = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
            commonAuthService.checkAuth(trxCode, trx);

            MerchantDetailDTO merchantDetail = merchantConnector.merchantDetail(merchantId, trx.getInitiativeId());

            commonAuthService.checkWalletStatus(trx.getInitiativeId(), trx.getUserId());

            setTrxFields(merchantId, authBarCodePaymentDTO, trx, merchantDetail, acquirerId);

            commonAuthService.checkTrxStatusToInvokePreAuth(trx);

            AuthPaymentDTO authPaymentDTO = commonAuthService.invokeRuleEngine(trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, merchantId,authPaymentDTO.getRewardCents(), authPaymentDTO.getRejectionReasons());
            authPaymentDTO.setResidualBudgetCents(CommonPaymentUtilities.calculateResidualBudget(authPaymentDTO.getRewards()));
            authPaymentDTO.setRejectionReasons(Collections.emptyList());
            Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(authBarCodePaymentDTO.getAmountCents(), authPaymentDTO.getRewardCents());
            authPaymentDTO.setSplitPayment(splitPaymentAndResidualAmountCents.getKey());
            authPaymentDTO.setResidualAmountCents(splitPaymentAndResidualAmountCents.getValue());
            return authPaymentDTO;
        } catch (RuntimeException e) {
            logErrorAuthorizedPayment(trxCode, merchantId);
            throw e;
        }
    }

    private void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId,Long rewardCents, List<String> rejectionReasons){
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, rewardCents, rejectionReasons);
    }

    private void logErrorAuthorizedPayment(String trxCode, String merchantId){
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    private static void setTrxFields(String merchantId, AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     TransactionInProgress trx, MerchantDetailDTO merchantDetail, String acquirerId) {
        trx.setAmountCents(authBarCodePaymentDTO.getAmountCents());
        trx.setEffectiveAmountCents(authBarCodePaymentDTO.getAmountCents());
        trx.setIdTrxAcquirer(authBarCodePaymentDTO.getIdTrxAcquirer());
        trx.setMerchantId(merchantId);
        trx.setBusinessName(merchantDetail.getBusinessName());
        trx.setMerchantFiscalCode(merchantDetail.getFiscalCode());
        trx.setVat(merchantDetail.getVatNumber());
        trx.setAcquirerId(acquirerId);
        trx.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
    }
}
