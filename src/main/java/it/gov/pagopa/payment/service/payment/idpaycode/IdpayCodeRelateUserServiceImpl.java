package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdpayCodeRelateUserServiceImpl implements IdpayCodeRelateUserService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonPreAuthServiceImpl commonPreAuthService;
    private final EncryptRestConnector encryptRestConnector;
    private final RelateUserResponseMapper relateUserResponseMapper;

    public IdpayCodeRelateUserServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                          @Qualifier("commonPreAuth") CommonPreAuthServiceImpl commonPreAuthService,
                                          EncryptRestConnector encryptRestConnector,
                                          RelateUserResponseMapper relateUserResponseMapper) {
        this.transactionInProgressRepository = transactionInProgressRepository;

        this.commonPreAuthService = commonPreAuthService;
        this.encryptRestConnector = encryptRestConnector;
        this.relateUserResponseMapper = relateUserResponseMapper;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, String fiscalCode) {
        String userId = retrieveUserId(fiscalCode);

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

        TransactionInProgress trxInProgress = commonPreAuthService.relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trxId, userId, RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        commonPreAuthService.auditLogRelateUser(trxInProgress, RewardConstants.TRX_CHANNEL_IDPAYCODE);

        return relateUserResponseMapper.transactionMapper(trxInProgress);
    }

    private String retrieveUserId(String fiscalCode) {
        EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
        return encryptedCfDTO.getToken();
    }
}
