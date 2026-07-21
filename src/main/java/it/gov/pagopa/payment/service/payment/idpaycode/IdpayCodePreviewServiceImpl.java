package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.enums.ProductCategory;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InvalidProductCategoryException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdpayCodePreviewServiceImpl implements IdpayCodePreviewService{
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final PaymentInstrumentConnector paymentInstrumentConnector;
    private final CommonPreAuthServiceImpl commonPreAuthService;
    private final AuthPaymentMapper authPaymentMapper;
    private final AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper;
    private final AuditUtilities auditUtilities;
    private final MerchantConnector merchantConnector;

    public IdpayCodePreviewServiceImpl(TransactionRepository transactionRepository,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       PaymentInstrumentConnector paymentInstrumentConnector,
                                       @Qualifier("commonPreAuth") CommonPreAuthServiceImpl commonPreAuthService,
                                       AuthPaymentMapper authPaymentMapper,
                                       AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper,
                                       AuditUtilities auditUtilities,
                                       MerchantConnector merchantConnector) {
        this.transactionRepository = transactionRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
        this.commonPreAuthService = commonPreAuthService;
        this.authPaymentMapper = authPaymentMapper;
        this.authPaymentIdpayCodeMapper = authPaymentIdpayCodeMapper;
        this.auditUtilities = auditUtilities;
        this.merchantConnector = merchantConnector;
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String merchantId, String initiativeId) {
        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));
        transactionRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

        if (!trx.getInitiativeId().equals(initiativeId)) {
            log.error("[IDPAY_CODE_PREVIEW] Transaction [{}] belongs to initiative [{}] but requested for initiative [{}]",
                    trxId, trx.getInitiativeId(), initiativeId);
            throw new TransactionNotFoundOrExpiredException(
                    "Cannot find transaction with transactionId [%s] for initiative [%s]".formatted(trxId, initiativeId));
        }

        if(!trx.getMerchantId().equals(merchantId)){
            throw new MerchantOrAcquirerNotAllowedException(
                    PaymentConstants.ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED,
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(),merchantId));
        }

        merchantConnector.merchantDetail(merchantId, trx.getInitiativeId());

        if (trx.getPointOfSaleId() != null) {
            merchantConnector.getPointOfSale(merchantId, trx.getPointOfSaleId(), initiativeId);
        }

        Long calculatedVoucher = calculateVoucherAmount(trx.getProductType(), trx.getAmountCents());
        trx.setVoucherAmountCents(calculatedVoucher);

        transactionInProgressRepository.save(trx);

        if(trx.getUserId() == null){
            return authPaymentMapper.transactionMapper(trx);
        }

        SecondFactorDTO secondFactorDetails = paymentInstrumentConnector.getSecondFactor(trx.getUserId());

        commonPreAuthService.checkPreAuth(trx.getUserId(), trx);

        AuthPaymentDTO authPaymentDTO = commonPreAuthService.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED);

        auditUtilities.logPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), RewardConstants.TRX_CHANNEL_IDPAYCODE);
        return authPaymentIdpayCodeMapper.authPaymentMapper(authPaymentDTO, secondFactorDetails.getSecondFactor());
    }

    private Long calculateVoucherAmount(String productType, Long amountCents) {
        if (productType == null) {
            throw new InvalidProductCategoryException("Product type cannot be null");
        }

        ProductCategory category;
        try {
            category = ProductCategory.valueOf(productType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidProductCategoryException(
                    "Product category [%s] is not eligible for this initiative".formatted(productType)
            );
        }

        long maxLimit = switch (category) {
            case DS, DTS, DTSC -> 7000L;
            case DT, DTC -> 3000L;
        };

        long calculatedPercentage = Math.round(amountCents * 0.70);

        return Math.min(calculatedPercentage, maxLimit);
    }
}
