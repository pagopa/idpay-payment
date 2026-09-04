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
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl implements BarCodeAuthPaymentService {

    private static final String PRODUCT_TYPE_KEY = "productType";

    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;
    private final MerchantConnector merchantConnector;
    private final TransactionRepository transactionRepository;
    private final CommonAuthServiceImpl commonAuthService;
    private final DecryptRestConnector decryptRestConnector;
    private final BarCodeAdditionalPropertiesValidationResolver additionalPropertiesValidationResolver;
    protected final AuditUtilities auditUtilities;

    public BarCodeAuthPaymentServiceImpl(BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         MerchantConnector merchantConnector,
                                         TransactionRepository transactionRepository,
                                         CommonAuthServiceImpl commonAuthService,
                                         DecryptRestConnector decryptRestConnector,
                                         BarCodeAdditionalPropertiesValidationResolver additionalPropertiesValidationResolver,
                                         AuditUtilities auditUtilities) {
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
        this.merchantConnector = merchantConnector;
        this.transactionRepository = transactionRepository;
        this.commonAuthService = commonAuthService;
        this.decryptRestConnector = decryptRestConnector;
        this.additionalPropertiesValidationResolver = additionalPropertiesValidationResolver;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public PreviewPaymentResultDTO previewPayment(String initiativeId,
                                                  String trxCode,
                                                  Map<String, String> additionalProperties,
                                                  Long amountCents) {

        final Transaction transaction = transactionRepository.findByTrxCodeAndStatusNot(trxCode.toLowerCase(), SyncTrxStatus.CANCELLED)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        "Cannot find transaction with trxCode [%s]".formatted(trxCode.toLowerCase())));

        if (!Objects.equals(transaction.getInitiativeId(), initiativeId)) {
            throw new TransactionNotFoundOrExpiredException(
                    "Cannot find transaction with trxCode [%s] for initiative [%s]".formatted(
                            trxCode.toLowerCase(), initiativeId));
        }

        if (!(SyncTrxStatus.CREATED.equals(transaction.getStatus()) || SyncTrxStatus.IDENTIFIED.equals(transaction.getStatus()))) {
            throw new OperationNotAllowedException(ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                    "Cannot operate on transaction with transactionId [%s] in status %s".formatted(transaction.getId(),transaction.getStatus()));
        }

        transaction.setAmountCents(amountCents);
        transaction.setAdditionalProperties(validateAdditionalProperties(
                transaction,
                additionalProperties,
                BarCodeAdditionalPropertiesOperation.PREVIEW));
        transaction.setProductType(transaction.getAdditionalProperties().get(PRODUCT_TYPE_KEY));

        final AuthPaymentDTO preview = commonAuthService
                .previewPayment(transaction, transaction.getUserId());

        if (preview.getRewardCents() < 0L) {
            log.info("[PREVIEW_TRANSACTION] Cannot preview transaction with negative reward: {}", preview.getRewardCents());
            throw new TransactionInvalidException(ExceptionCode.REWARD_NOT_VALID, "Cannot preview transaction with negative reward [%s]".formatted(preview.getRewardCents()));
        }

        final long residualAmountCents = amountCents - preview.getRewardCents();

        if (residualAmountCents < 0L) {
            log.info("[PREVIEW_TRANSACTION] Residual amountCents calculated negative: original = {}, reward = {}", amountCents, preview.getRewardCents());
            throw new TransactionInvalidException(ExceptionCode.REWARD_NOT_VALID, "Residual amountCents cannot be negative: amountCents [%s], rewardCents [%s]".formatted(amountCents, preview.getRewardCents()));
        }

        final String userCf = decryptRestConnector.getPiiByToken(transaction.getUserId()).getPii();

        return PreviewPaymentResultDTO.builder()
                .trxCode(preview.getTrxCode())
                .trxDate(preview.getTrxDate())
                .status(preview.getStatus())
                .originalAmountCents(amountCents)
                .rewardCents(preview.getRewardCents())
                .residualAmountCents(residualAmountCents)
                .userId(userCf)
                .additionalProperties(transaction.getAdditionalProperties())
                .extendedAuthorization(transaction.getExtendedAuthorization())
                .build();
    }

    @Override
    public AuthPaymentDTO authPayment(String initiativeId, String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId) {
        try {
            if (authBarCodePaymentDTO.getAmountCents() <= 0L) {
                log.info("[AUTHORIZE_TRANSACTION] Cannot authorize transaction with invalid amount: [{}]", authBarCodePaymentDTO.getAmountCents());
                throw new TransactionInvalidException(ExceptionCode.AMOUNT_NOT_VALID, "Cannot authorize transaction with invalid amount [%s]".formatted(authBarCodePaymentDTO.getAmountCents()));
            }

            Transaction transaction = barCodeAuthorizationExpiredService.findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNot(trxCode.toLowerCase());

            if (transaction == null || !Objects.equals(transaction.getInitiativeId(), initiativeId)) {
                throw new TransactionNotFoundOrExpiredException("Cannot find transaction with trxCode [%s] for initiative [%s]".formatted(trxCode, initiativeId));
            }
            commonAuthService.checkAuth(trxCode, transaction);

            transaction.setAdditionalProperties(validateAdditionalProperties(
                    transaction,
                    authBarCodePaymentDTO.getAdditionalProperties(),
                    BarCodeAdditionalPropertiesOperation.AUTHORIZE));
            transaction.setProductType(transaction.getAdditionalProperties().get(PRODUCT_TYPE_KEY));

            PointOfSaleDTO pointOfSaleDTO = merchantConnector.getPointOfSale(
                    merchantId, pointOfSaleId, transaction.getInitiativeId());

            WalletDTO walletDTO = commonAuthService.checkWalletStatusAndReturn(transaction.getInitiativeId(), transaction.getUserId());

            setTrxFields(merchantId, authBarCodePaymentDTO, transaction, pointOfSaleDTO, acquirerId, pointOfSaleId, walletDTO.getFamilyId());

            commonAuthService.checkTrxStatusToInvokePreAuth(transaction);

            AuthPaymentDTO authPaymentDTO = commonAuthService.invokeRuleEngine(transaction);

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

    private Map<String, String> validateAdditionalProperties(Transaction transaction,
                                                             Map<String, String> additionalProperties,
                                                             BarCodeAdditionalPropertiesOperation operation) {
        Map<String, String> validatedAdditionalProperties = additionalPropertiesValidationResolver
                .resolve(transaction.getInitiativeId())
                .validateAndEnrich(additionalProperties, operation, transaction.getInitiativeId());
        return Objects.requireNonNullElse(validatedAdditionalProperties, Collections.emptyMap());
    }

    private void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId, Long rewardCents, List<String> rejectionReasons) {
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, rewardCents, rejectionReasons);
    }

    private void logErrorAuthorizedPayment(String trxCode, String merchantId) {
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    private static void setTrxFields(String merchantId, AuthBarCodePaymentDTO authBarCodePaymentDTO,
                                     Transaction transaction, PointOfSaleDTO pointOfSaleDTO, String acquirerId, String pointOfSaleId,
                                     String familyId) {
        transaction.setAmountCents(authBarCodePaymentDTO.getAmountCents());
        transaction.setEffectiveAmountCents(authBarCodePaymentDTO.getAmountCents());
        transaction.setIdTrxAcquirer(authBarCodePaymentDTO.getIdTrxAcquirer());
        transaction.setMerchantId(merchantId);
        transaction.setBusinessName(pointOfSaleDTO.getBusinessName());
        transaction.setMerchantFiscalCode(pointOfSaleDTO.getFiscalCode());
        transaction.setVat(pointOfSaleDTO.getVatNumber());
        transaction.setFranchiseName(pointOfSaleDTO.getFranchiseName());
        transaction.setPointOfSaleType(pointOfSaleDTO.getType().name());
        transaction.setAcquirerId(acquirerId);
        transaction.setAmountCurrency(PaymentConstants.CURRENCY_EUR);
        transaction.setPointOfSaleId(pointOfSaleId);
        transaction.setFamilyId(familyId);
    }
}
