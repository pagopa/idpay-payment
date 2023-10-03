package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserRequestMapper;
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
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final CommonPreAuthService commonPreAuthService;
    private final RelateUserRequestMapper relateUserRequestMapper;

    public IdpayCodePreAuthServiceImpl(TransactionInProgressRepository transactionInProgressRepository, CommonPreAuthService commonPreAuthService, RelateUserRequestMapper relateUserRequestMapper) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.commonPreAuthService = commonPreAuthService;
        this.relateUserRequestMapper = relateUserRequestMapper;
    }

    @Override
    public UserRelateResponse relateUser(String trxId, String userId) {
        TransactionInProgress trx = transactionInProgressRepository.findById(trxId)
                .orElseThrow(() -> new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                        "Cannot find transaction with transactionId [%s]".formatted(trxId)));

        TransactionInProgress trxInProgress = commonPreAuthService.relateUser(trx, userId);

        transactionInProgressRepository.updateTrxRelateUser(trxId, userId, SyncTrxStatus.IDENTIFIED); //TODO 1921 refactor?

        return relateUserRequestMapper.transactionMapper(trxInProgress);
    }
}
