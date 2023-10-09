package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
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
    private final EncryptRestConnector encryptRestConnector;
    private final RelateUserResponseMapper relateUserResponseMapper;
    public IdpayCodePreAuthServiceImpl(@Value("${app.idpayCode.expirations.authorizationMinutes}") long authorizationExpirationMinutes,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       RewardCalculatorConnector rewardCalculatorConnector,
                                       AuditUtilities auditUtilities,
                                       WalletConnector walletConnector,
                                       EncryptRestConnector encryptRestConnector, RelateUserResponseMapper relateUserResponseMapper) {
        super(authorizationExpirationMinutes, transactionInProgressRepository, rewardCalculatorConnector, auditUtilities, walletConnector);
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.encryptRestConnector = encryptRestConnector;
        this.relateUserResponseMapper = relateUserResponseMapper;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        String userId = retrieveUserId(request.getFiscalCode());

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElse(null);

        if(trx == null){
            return null;
        }

        TransactionInProgress trxInProgress = relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trxId, userId, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        return relateUserResponseMapper.transactionMapper(trxInProgress);
    }

    private String retrieveUserId(String fiscalCode) {
        EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
        return encryptedCfDTO.getToken();
    }
}
