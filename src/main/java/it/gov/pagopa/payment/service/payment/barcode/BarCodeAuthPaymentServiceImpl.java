package it.gov.pagopa.payment.service.payment.barcode;

import io.micrometer.common.util.StringUtils;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.payment.utils.Utilities.sanitizeString;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl implements BarCodeAuthPaymentService {

    private final PaymentCheckService paymentCheckService;
    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonAuthServiceImpl commonAuthService;
    private final DecryptRestConnector decryptRestConnector;
    protected final AuditUtilities auditUtilities;

    private static final String PRODUCT_GTIN_KEY = "productGtin";

    public BarCodeAuthPaymentServiceImpl(PaymentCheckService paymentCheckService,
                                         BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         MerchantConnector merchantConnector,
                                         TransactionInProgressRepository transactionInProgressRepository,
                                         CommonAuthServiceImpl commonAuthService,
                                         DecryptRestConnector decryptRestConnector,
                                         AuditUtilities auditUtilities) {
        this.paymentCheckService = paymentCheckService;
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.commonAuthService = commonAuthService;
        this.decryptRestConnector = decryptRestConnector;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public PreviewPaymentDTO previewPayment(String productGtin, String trxCode, Long amountCents) {

        final TransactionInProgress transactionInProgress =
                transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
                        .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                                "Cannot find transaction with trxCode [%s]".formatted(trxCode.toLowerCase())));
        transactionInProgress.setAmountCents(amountCents);

        final AuthPaymentDTO preview = commonAuthService
                .previewPayment(transactionInProgress, transactionInProgress.getUserId());

        if (preview.getRewardCents() < 0L) {
            log.info("[PREVIEW_TRANSACTION] Cannot preview transaction with negative reward: {}", preview.getRewardCents());
            throw new TransactionInvalidException(ExceptionCode.REWARD_NOT_VALID, "Cannot preview transaction with negative reward [%s]".formatted(preview.getRewardCents()));
        }

        final long residualAmountCents = amountCents - preview.getRewardCents();

        if (residualAmountCents < 0L) {
            log.info("[PREVIEW_TRANSACTION] Residual amountCents calculated negative: original = {}, reward = {}", amountCents, preview.getRewardCents());
            throw new TransactionInvalidException(ExceptionCode.REWARD_NOT_VALID, "Residual amountCents cannot be negative: amountCents [%s], rewardCents [%s]".formatted(amountCents, preview.getRewardCents()));
        }

        final String userCf = decryptRestConnector.getPiiByToken(transactionInProgress.getUserId()).getPii();

        ProductDTO productDTO = paymentCheckService.validateProduct(productGtin);

        transactionInProgress.setAdditionalProperties(buildAdditionalProperties(productDTO));

        return PreviewPaymentDTO.builder()
                .trxCode(preview.getTrxCode())
                .trxDate(preview.getTrxDate())
                .status(preview.getStatus())
                .originalAmountCents(amountCents)
                .rewardCents(preview.getRewardCents())
                .residualAmountCents(residualAmountCents)
                .userId(userCf)
                .extendedAuthorization(transactionInProgress.getExtendedAuthorization())
                .build();
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId) {
        try {
            if (authBarCodePaymentDTO.getAmountCents() <= 0L) {
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid amount: [{}]", authBarCodePaymentDTO.getAmountCents());
                throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot authorize transaction with invalid amount [%s]".formatted(authBarCodePaymentDTO.getAmountCents()));
            }

            if(authBarCodePaymentDTO.getAdditionalProperties() == null || StringUtils.isEmpty(authBarCodePaymentDTO.getAdditionalProperties().get(PRODUCT_GTIN_KEY))){
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid AdditionalProperties");
                throw new TransactionInvalidException(ExceptionCode.TRX_ADDITIONAL_PROPERTIES_NOT_EXIST, "Cannot authorize transaction with invalid AdditionalProperties");
            }

            TransactionInProgress trx = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
            commonAuthService.checkAuth(trxCode, trx);

            String sanitizedProductGtin = sanitizeString(authBarCodePaymentDTO.getAdditionalProperties().get(PRODUCT_GTIN_KEY));
            ProductDTO productDTO = paymentCheckService.validateProduct(sanitizedProductGtin);

            trx.setAdditionalProperties(buildAdditionalProperties(productDTO));

            PointOfSaleDTO pointOfSaleDTO = merchantConnector.getPointOfSale(merchantId, pointOfSaleId);

            commonAuthService.checkWalletStatus(trx.getInitiativeId(), trx.getUserId());

            setTrxFields(merchantId, authBarCodePaymentDTO, trx, pointOfSaleDTO, acquirerId, pointOfSaleId);

            commonAuthService.checkTrxStatusToInvokePreAuth(trx);

            AuthPaymentDTO authPaymentDTO = commonAuthService.invokeRuleEngine(trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, merchantId, authPaymentDTO.getRewardCents(), authPaymentDTO.getRejectionReasons());
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

    private Map<String, String> buildAdditionalProperties(ProductDTO productDTO){
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("productName", productDTO.getProductName());
        additionalProperties.put(PRODUCT_GTIN_KEY, productDTO.getGtinCode());
        return additionalProperties;
    }

    private void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId, Long rewardCents, List<String> rejectionReasons) {
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, rewardCents, rejectionReasons);
    }

    private void logErrorAuthorizedPayment(String trxCode, String merchantId) {
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    private static void setTrxFields(String merchantId, AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     TransactionInProgress trx, PointOfSaleDTO pointOfSaleDTO, String acquirerId, String pointOfSaleId) {
        trx.setAmountCents(authBarCodePaymentDTO.getAmountCents());
        trx.setEffectiveAmountCents(authBarCodePaymentDTO.getAmountCents());
        trx.setIdTrxAcquirer(authBarCodePaymentDTO.getIdTrxAcquirer());
        trx.setMerchantId(merchantId);
        trx.setBusinessName(pointOfSaleDTO.getBusinessName());
        trx.setMerchantFiscalCode(pointOfSaleDTO.getFiscalCode());
        trx.setVat(pointOfSaleDTO.getVatNumber());
        trx.setFranchiseName(pointOfSaleDTO.getFranchiseName());
        trx.setPointOfSaleType(pointOfSaleDTO.getType().name());
        trx.setAcquirerId(acquirerId);
        trx.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
        trx.setPointOfSaleId(pointOfSaleId);
    }
}
