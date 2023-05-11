package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.ErrorNotifierService;
import it.gov.pagopa.payment.service.TransactionNotifierService;
import it.gov.pagopa.payment.service.TransactionNotifierServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeConfirmationServiceImpl implements QRCodeConfirmationService {

    private final TransactionInProgressRepository repository;
    private final TransactionInProgress2TransactionResponseMapper mapper;
    private final TransactionNotifierService notifierService;
    private final ErrorNotifierService errorNotifierService;

    public QRCodeConfirmationServiceImpl(TransactionInProgressRepository repository, TransactionInProgress2TransactionResponseMapper mapper, TransactionNotifierService notifierService, ErrorNotifierService errorNotifierService) {
        this.repository = repository;
        this.mapper = mapper;
        this.notifierService = notifierService;
        this.errorNotifierService= errorNotifierService;
    }

    @Override
    public TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId) {
        TransactionInProgress trx = repository.findByIdThrottled(trxId);

        if (trx == null) {
            throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "[CONFIRM_PAYMENT] Cannot found transaction having id: " + trxId);
        }
        if(!trx.getMerchantId().equals(merchantId) || !trx.getAcquirerId().equals(acquirerId)){
            throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "[CONFIRM_PAYMENT] Requesting merchantId (%s through acquirer %s) not allowed to operate on transaction having id %s".formatted(merchantId, acquirerId, trxId));
        }
        if(!SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
            throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "[CONFIRM_PAYMENT] Cannot confirm transaction having id %s: actual status is %s".formatted(trxId, trx.getStatus()));
        }

        trx.setStatus(SyncTrxStatus.REWARDED);

        sendConfirmPaymentNotification(trx);

        repository.deleteById(trxId);

        return mapper.apply(trx);
    }

    private void sendConfirmPaymentNotification(TransactionInProgress trx) {
        try {
            log.info("[QR_CODE_CONFIRM_PAYMENT][SEND_NOTIFICATION] Sending Confirmation Payment event to Notification: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getMerchantId(), trx.getAcquirerId());
            if (!notifierService.notifyByMerch(trx)) {
                throw new IllegalStateException("[QR_CODE_CONFIRM_PAYMENT] Something gone wrong while Confirm Payment notify");
            }
        } catch (Exception e) {
            if(!errorNotifierService.notifyConfirmPayment(
                    TransactionNotifierServiceImpl.buildMessage(trx, trx.getMerchantId()),
                    "[QR_CODE_CONFIRM_PAYMENT] An error occurred while publishing the confirmation Payment result: trxId %s - merchantId %s - acquirerId %s".formatted(trx.getId(), trx.getMerchantId(), trx.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[QR_CODE_CONFIRM_PAYMENT][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getUserId(), trx.getAcquirerId(), e);
            }
        }
    }
}
