package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesOperation;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationResolver;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl implements BarCodeAuthPaymentService {

    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    private final CommonAuthServiceImpl commonAuthService;
    private final DecryptRestConnector decryptRestConnector;
    private final BarCodeAdditionalPropertiesValidationResolver additionalPropertiesResolver;
    private final AuditUtilities auditUtilities;

    public BarCodeAuthPaymentServiceImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository transactionInProgressRepository,
            BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
            MerchantConnector merchantConnector,
            CommonAuthServiceImpl commonAuthService,
            DecryptRestConnector decryptRestConnector,
            BarCodeAdditionalPropertiesValidationResolver additionalPropertiesResolver,
            AuditUtilities auditUtilities) {

        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
        this.commonAuthService = commonAuthService;
        this.decryptRestConnector = decryptRestConnector;
        this.additionalPropertiesResolver = additionalPropertiesResolver;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public PreviewPaymentResultDTO previewPayment(
            String trxCode,
            Map<String, String> additionalProperties,
            Long amountCents) {

        TransactionInProgress mongo = loadMongo(trxCode);
        Transaction postgres = loadPostgres(trxCode);

        applyAmount(mongo, amountCents);
        applyAmount(postgres, amountCents);

        AuthPaymentDTO previewMongo = commonAuthService.previewPayment(mongo, mongo.getUserId());
        AuthPaymentDTO previewPostgres = commonAuthService.previewPayment(postgres, postgres.getUserId());

        long residual = validatePreview(previewMongo, amountCents);

        //final String userCf = decryptRestConnector.getPiiByToken(transactionInProgress.getUserId()).getPii();
        final String userCf = "userCF";

        Map<String, String> validatedPropsMongo = validateAdditionalProperties(mongo, additionalProperties, BarCodeAdditionalPropertiesOperation.PREVIEW);
        Map<String, String> validatedPropsPostgres = validateAdditionalProperties(postgres, additionalProperties, BarCodeAdditionalPropertiesOperation.PREVIEW);

        mongo.setAdditionalProperties(validatedPropsMongo);
        postgres.setAdditionalProperties(validatedPropsPostgres);

        return PreviewPaymentResultDTO.builder()
                .trxCode(previewMongo.getTrxCode())
                .trxDate(previewMongo.getTrxDate())
                .status(previewMongo.getStatus())
                .originalAmountCents(amountCents)
                .rewardCents(previewMongo.getRewardCents())
                .residualAmountCents(residual)
                .userId(userCf)
                .additionalProperties(mongo.getAdditionalProperties())
                .extendedAuthorization(mongo.getExtendedAuthorization())
                .build();
    }

    private TransactionInProgress loadMongo(String trxCode) {
        return transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
                .orElseThrow(() -> notFound(trxCode));
    }

    private Transaction loadPostgres(String trxCode) {
        return transactionRepository.findByTrxCode(trxCode.toLowerCase())
                .orElseThrow(() -> notFound(trxCode));
    }

    private TransactionNotFoundOrExpiredException notFound(String trxCode) {
        return new TransactionNotFoundOrExpiredException(
                "Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }

    private void applyAmount(TransactionInProgress trx, Long amount) {
        trx.setAmountCents(amount);
    }

    private void applyAmount(Transaction trx, Long amount) {
        trx.setAmountCents(amount);
    }

    private long validatePreview(AuthPaymentDTO preview, Long amountCents) {
        if (preview.getRewardCents() < 0) {
            throw new TransactionInvalidException(
                    PaymentConstants.ExceptionCode.REWARD_NOT_VALID,
                    "Negative reward [%s]".formatted(preview.getRewardCents()));
        }

        long residual = amountCents - preview.getRewardCents();

        if (residual < 0) {
            throw new TransactionInvalidException(
                    PaymentConstants.ExceptionCode.REWARD_NOT_VALID,
                    "Negative residual amount");
        }

        return residual;
    }

    @Override
    public AuthPaymentDTO authPayment(
            String trxCode,
            AuthBarCodePaymentDTO request,
            String merchantId,
            String pointOfSaleId,
            String acquirerId) {
        try {
            if (request.getAmountCents() <= 0L) {
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid amount: [{}]", request.getAmountCents());
                throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot authorize transaction with invalid amount [%s]".formatted(request.getAmountCents()));
            }

            TransactionInProgress mongo = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());
            Transaction postgres = transactionRepository.findByTrxCodeAndTrxEndDateGreaterThanEqual(trxCode.toLowerCase(), OffsetDateTime.now())
                    .orElseThrow(() -> notFound(trxCode));

            commonAuthService.checkAuth(trxCode, mongo);
            commonAuthService.checkAuth(trxCode, postgres);

            mongo.setAdditionalProperties(validateAdditionalProperties(
                    mongo,
                    request.getAdditionalProperties(),
                    BarCodeAdditionalPropertiesOperation.AUTHORIZE));

            postgres.setAdditionalProperties(validateAdditionalProperties(
                    postgres,
                    request.getAdditionalProperties(),
                    BarCodeAdditionalPropertiesOperation.AUTHORIZE));

            PointOfSaleDTO pointOfSaleDTO = merchantConnector.getPointOfSale(merchantId, pointOfSaleId);
            WalletDTO walletDTO = commonAuthService.checkWalletStatusAndReturn(mongo.getInitiativeId(), mongo.getUserId());

            setTrxFields(merchantId, request, mongo, pointOfSaleDTO, acquirerId, pointOfSaleId, walletDTO.getFamilyId());
            setTrxFields(merchantId, request, postgres, pointOfSaleDTO, acquirerId, pointOfSaleId, walletDTO.getFamilyId());

            commonAuthService.checkTrxStatusToInvokePreAuth(mongo);
            commonAuthService.checkTrxStatusToInvokePreAuth(postgres);

            AuthPaymentDTO authPaymentDTO = commonAuthService.invokeRuleEngine(postgres, mongo);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, merchantId, authPaymentDTO.getRewardCents(), authPaymentDTO.getRejectionReasons());
            authPaymentDTO.setResidualBudgetCents(CommonPaymentUtilities.calculateResidualBudget(authPaymentDTO.getRewards()));
            authPaymentDTO.setRejectionReasons(Collections.emptyList());
            Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(request.getAmountCents(), authPaymentDTO.getRewardCents());
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
        Map<String, String> validatedAdditionalProperties = additionalPropertiesResolver
                .resolve(trx.getInitiativeId())
                .validateAndEnrich(additionalProperties, operation);
        if (validatedAdditionalProperties == null) {
            return Collections.emptyMap();
        }
        return validatedAdditionalProperties;
    }

    private Map<String, String> validateAdditionalProperties(Transaction transaction,
                                                             Map<String, String> additionalProperties,
                                                             BarCodeAdditionalPropertiesOperation operation) {
        Map<String, String> validatedAdditionalProperties = additionalPropertiesResolver
                .resolve(transaction.getInitiativeId())
                .validateAndEnrich(additionalProperties, operation);
        if (validatedAdditionalProperties == null) {
            return Collections.emptyMap();
        }
        return validatedAdditionalProperties;
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

    private static void setTrxFields(String merchantId,
                                     AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     Transaction trx,
                                     PointOfSaleDTO pointOfSaleDTO,
                                     String acquirerId,
                                     String pointOfSaleId,
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
        trx.setUpdateDate(LocalDateTime.now());
    }
}
