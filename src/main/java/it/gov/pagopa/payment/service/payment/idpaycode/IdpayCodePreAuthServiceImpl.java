package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.forbidden.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePreAuthServiceImpl extends CommonPreAuthServiceImpl implements IdpayCodePreAuthService {

    private final EncryptRestConnector encryptRestConnector;
    private final RelateUserResponseMapper relateUserResponseMapper;
    private final AuthPaymentMapper authPaymentMapper;
    private final PaymentInstrumentConnector paymentInstrumentConnector;
    private final AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper;
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public IdpayCodePreAuthServiceImpl(@Value("${app.common.expirations.authorizationMinutes}") long authorizationExpirationMinutes,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector,
                                       AuditUtilities auditUtilities,
                                       WalletConnector walletConnector,
                                       EncryptRestConnector encryptRestConnector,
                                       RelateUserResponseMapper relateUserResponseMapper,
                                       AuthPaymentMapper authPaymentMapper,
                                       PaymentInstrumentConnector paymentInstrumentConnector,
                                       AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper) {
        super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector);
        this.encryptRestConnector = encryptRestConnector;
        this.relateUserResponseMapper = relateUserResponseMapper;
        this.authPaymentMapper = authPaymentMapper;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
        this.authPaymentIdpayCodeMapper = authPaymentIdpayCodeMapper;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, String fiscalCode) {
        String userId = retrieveUserId(fiscalCode);

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

        TransactionInProgress trxInProgress = relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trxId, userId, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        return relateUserResponseMapper.transactionMapper(trxInProgress);
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String merchantId) {

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

        if(!trx.getMerchantId().equals(merchantId)){
            throw new MerchantOrAcquirerNotAllowedException(
                    ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED,
                    "The merchant id [%s] of the trx, is not equals to the merchant id [%s]".formatted(trx.getMerchantId(),merchantId));

        }

        if(trx.getUserId() == null){
            return authPaymentMapper.transactionMapper(trx);
        }

        checkPreAuth(trx.getTrxCode(), trx.getUserId(), trx);
        AuthPaymentDTO authPaymentDTO = super.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        SecondFactorDTO secondFactorDetails = paymentInstrumentConnector.getSecondFactor(trx.getUserId());

        auditUtilities.logPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), RewardConstants.TRX_CHANNEL_IDPAYCODE);
        return authPaymentIdpayCodeMapper.authPaymentMapper(authPaymentDTO, secondFactorDetails.getSecondFactor());
    }

    private String retrieveUserId(String fiscalCode) {
        EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
        return encryptedCfDTO.getToken();
    }
}
