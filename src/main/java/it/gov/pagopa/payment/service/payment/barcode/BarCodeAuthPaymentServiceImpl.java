package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesOperation;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationInput;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationResolver;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl implements BarCodeAuthPaymentService {

    private static final String PRODUCT_NAME_KEY = "productName";
    private static final String PRODUCT_GTIN_KEY = "productGtin";

    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonAuthServiceImpl commonAuthService;
    private final DecryptRestConnector decryptRestConnector;
    private final BarCodeAdditionalPropertiesValidationResolver additionalPropertiesValidationResolver;
    protected final AuditUtilities auditUtilities;

    public BarCodeAuthPaymentServiceImpl(BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         MerchantConnector merchantConnector,
                                         TransactionInProgressRepository transactionInProgressRepository,
                                         CommonAuthServiceImpl commonAuthService,
                                         DecryptRestConnector decryptRestConnector,
                                         BarCodeAdditionalPropertiesValidationResolver additionalPropertiesValidationResolver,
                                         AuditUtilities auditUtilities) {
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.commonAuthService = commonAuthService;
        this.decryptRestConnector = decryptRestConnector;
        this.additionalPropertiesValidationResolver = additionalPropertiesValidationResolver;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public PreviewPaymentDTO previewPayment(String trxCode, Map<String, String> additionalProperties, Long amountCents) {

        final TransactionInProgress transactionInProgress =
                transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
                        .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                                "Cannot find transaction with trxCode [%s]".formatted(trxCode.toLowerCase())));
        transactionInProgress.setAmountCents(amountCents);
        transactionInProgress.setAdditionalProperties(validateAdditionalProperties(
                transactionInProgress,
                additionalProperties,
                BarCodeAdditionalPropertiesOperation.PREVIEW));

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

        return PreviewPaymentDTO.builder()
                .trxCode(preview.getTrxCode())
                .trxDate(preview.getTrxDate())
                .status(preview.getStatus())
                .originalAmountCents(amountCents)
                .rewardCents(preview.getRewardCents())
                .residualAmountCents(residualAmountCents)
                .userId(userCf)
                .productName(getAdditionalProperty(transactionInProgress, PRODUCT_NAME_KEY))
                .productGtin(getAdditionalProperty(transactionInProgress, PRODUCT_GTIN_KEY))
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

            TransactionInProgress trx = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
            if (trx == null) {
                commonAuthService.checkAuth(trxCode, null);
            }

            trx.setAdditionalProperties(validateAdditionalProperties(
                    trx,
                    authBarCodePaymentDTO.getAdditionalProperties(),
                    BarCodeAdditionalPropertiesOperation.AUTHORIZE));
            commonAuthService.checkAuth(trxCode, trx);

            PointOfSaleDTO pointOfSaleDTO = merchantConnector.getPointOfSale(merchantId, pointOfSaleId);

            WalletDTO walletDTO = commonAuthService.checkWalletStatusAndReturn(trx.getInitiativeId(), trx.getUserId());

            setTrxFields(merchantId, authBarCodePaymentDTO, trx, pointOfSaleDTO, acquirerId, pointOfSaleId, walletDTO.getFamilyId());

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

    private Map<String, String> validateAdditionalProperties(TransactionInProgress trx,
                                                             Map<String, String> additionalProperties,
                                                             BarCodeAdditionalPropertiesOperation operation) {
        Map<String, String> validatedAdditionalProperties = additionalPropertiesValidationResolver
                .resolve(trx.getInitiativeId())
                .validateAndEnrich(new BarCodeAdditionalPropertiesValidationInput(trx, additionalProperties, operation));
        if (validatedAdditionalProperties == null) {
            return Collections.emptyMap();
        }
        return validatedAdditionalProperties;
    }

    private static String getAdditionalProperty(TransactionInProgress transactionInProgress, String propertyName) {
        return transactionInProgress.getAdditionalProperties() != null
                ? transactionInProgress.getAdditionalProperties().get(propertyName)
                : null;
    }

    private void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId, Long rewardCents, List<String> rejectionReasons) {
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, rewardCents, rejectionReasons);
    }

    private void logErrorAuthorizedPayment(String trxCode, String merchantId) {
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    private static void setTrxFields(String merchantId, AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     TransactionInProgress trx, PointOfSaleDTO pointOfSaleDTO, String acquirerId, String pointOfSaleId,
                                     String familyId) {
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
        trx.setFamilyId(familyId);
    }
}
