package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.DetailsDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final AuditUtilities auditUtilities;
    private final EncryptRestConnector encryptRestConnector;
    private final RelateUserResponseMapper relateUserResponseMapper;
    private final AuthPaymentMapper authPaymentMapper;
    private final PaymentInstrumentConnector paymentInstrumentConnector;
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public IdpayCodePreAuthServiceImpl(@Value("${app.idpayCode.expirations.authorizationMinutes}") long authorizationExpirationMinutes,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector,
                                       AuditUtilities auditUtilities,
                                       WalletConnector walletConnector,
                                       EncryptRestConnector encryptRestConnector,
                                       RelateUserResponseMapper relateUserResponseMapper,
                                       AuthPaymentMapper authPaymentMapper,
                                       PaymentInstrumentConnector paymentInstrumentConnector
    ) {
        super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector);
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.auditUtilities = auditUtilities;
        this.encryptRestConnector = encryptRestConnector;
        this.relateUserResponseMapper = relateUserResponseMapper;
        this.authPaymentMapper = authPaymentMapper;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        String userId = retrieveUserId(request.getFiscalCode());

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new IllegalStateException(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED));

        TransactionInProgress trxInProgress = relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trxId, userId, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        return relateUserResponseMapper.transactionMapper(trxInProgress);
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String acquirerId, String merchantFiscalCode) {
        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new IllegalStateException(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED));

        if(trx.getUserId() == null){
            return authPaymentMapper.transactionMapper(trx);
        }

        checkPreAuth(trx.getTrxCode(), trx.getUserId(), trx);
        AuthPaymentDTO authPaymentDTO = previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        //TODO IDP-1926 review
        DetailsDTO secondFactorDetails = paymentInstrumentConnector.getSecondFactor(trx.getInitiativeId(), trx.getUserId());
        authPaymentDTO.setSecondFactor(secondFactorDetails.getSecondFactor()); //TODO check secondFactor is null(?

        auditUtilities.logPreviewTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), RewardConstants.TRX_CHANNEL_IDPAYCODE);
        return authPaymentDTO;
    }

    private String retrieveUserId(String fiscalCode) {
        EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
        return encryptedCfDTO.getToken();
    }
}
