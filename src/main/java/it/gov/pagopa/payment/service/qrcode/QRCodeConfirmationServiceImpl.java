package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.TransactionNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeConfirmationServiceImpl implements QRCodeConfirmationService {

    private final TransactionInProgressRepository repository;
    private final TransactionInProgress2TransactionResponseMapper mapper;
    private final TransactionNotifierService notifierService;

    public QRCodeConfirmationServiceImpl(TransactionInProgressRepository repository, TransactionInProgress2TransactionResponseMapper mapper, TransactionNotifierService notifierService) {
        this.repository = repository;
        this.mapper = mapper;
        this.notifierService = notifierService;
    }

    @Override
    public TransactionResponse confirmPayment(String trxId, String merchantId) {
        TransactionInProgress trx = repository.findByIdThrottled(trxId);

        if (trx == null) {
            throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "[CONFIRM_PAYMENT] Cannot found transaction having id: " + trxId);
        }
        if(!trx.getMerchantId().equals(merchantId)){
            throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "[CONFIRM_PAYMENT] Requesting merchantId (%s) not allowed to operate on transaction having id %s".formatted(merchantId, trxId));
        }
        if(!SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
            throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "[CONFIRM_PAYMENT] Cannot confirm transaction having id %s: actual status is %s".formatted(trxId, trx.getStatus()));
        }

        trx.setStatus(SyncTrxStatus.REWARDED);

        if(!notifierService.notify(trx)){
            log.warn("Cannot notify confirm operation on trx " + trxId);
        }

        repository.deleteById(trxId);

        return mapper.apply(trx);
    }
}
