package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdpayCodePreviewServiceImpl implements IdpayCodePreviewService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final PaymentInstrumentConnector paymentInstrumentConnector;
    private final CommonPreAuthServiceImpl commonPreAuthService;
    private final AuthPaymentMapper authPaymentMapper;
    private final AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper;
    private final AuditUtilities auditUtilities;
    public IdpayCodePreviewServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                       PaymentInstrumentConnector paymentInstrumentConnector,
                                       @Qualifier("commonPreAuth") CommonPreAuthServiceImpl commonPreAuthService,
                                       AuthPaymentMapper authPaymentMapper,
                                       AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper,
                                       AuditUtilities auditUtilities) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
        this.commonPreAuthService = commonPreAuthService;
        this.authPaymentMapper = authPaymentMapper;
        this.authPaymentIdpayCodeMapper = authPaymentIdpayCodeMapper;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String merchantId) {
        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

        if(!trx.getMerchantId().equals(merchantId)){
            throw new MerchantOrAcquirerNotAllowedException(
                    PaymentConstants.ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED,
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(),merchantId));
        }

        if(trx.getUserId() == null){
            return authPaymentMapper.transactionMapper(trx);
        }

        SecondFactorDTO secondFactorDetails = paymentInstrumentConnector.getSecondFactor(trx.getUserId());

        commonPreAuthService.checkPreAuth(trx.getUserId(), trx);
        AuthPaymentDTO authPaymentDTO = commonPreAuthService.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED);

        auditUtilities.logPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), RewardConstants.TRX_CHANNEL_IDPAYCODE);
        return authPaymentIdpayCodeMapper.authPaymentMapper(authPaymentDTO, secondFactorDetails.getSecondFactor());
    }
}
