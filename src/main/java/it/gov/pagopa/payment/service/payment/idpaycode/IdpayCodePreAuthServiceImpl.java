package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePreAuthServiceImpl implements IdpayCodePreAuthService {
    private static final String IDPAYCODE = "IDPAYCODE";
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonPreAuthService commonPreAuthService;
    private final RelateUserResponseMapper relateUserResponseMapper;

    public IdpayCodePreAuthServiceImpl(EncryptRestConnector encryptRestConnector,
                                       TransactionInProgressRepository transactionInProgressRepository,
                                       CommonPreAuthService commonPreAuthService,
                                       RelateUserResponseMapper relateUserResponseMapper) {
        this.encryptRestConnector = encryptRestConnector;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.commonPreAuthService = commonPreAuthService;
        this.relateUserResponseMapper = relateUserResponseMapper;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        String userId = retrieveUserId(request.getFiscalCode());

        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                        "Cannot find transaction with transactionId [%s]".formatted(trxId)));

        TransactionInProgress trxInProgress = commonPreAuthService.relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trxId, userId, IDPAYCODE);
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        commonPreAuthService.auditLogUserRelate(trx);

        return relateUserResponseMapper.transactionMapper(trxInProgress);
    }

    private String retrieveUserId(String fiscalCode) {
        String userId;
        try {
            EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
            userId = encryptedCfDTO.getToken();
            return userId;
        } catch (Exception e) {
            throw new ClientExceptionWithBody(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL SERVER ERROR",
                    "Error during encryption");
        }
    }
}
