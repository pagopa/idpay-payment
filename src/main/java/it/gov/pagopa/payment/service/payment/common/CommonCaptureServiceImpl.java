package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service("commonCapture")
public class CommonCaptureServiceImpl {
    private final TransactionInProgressRepository repository;
    private final TransactionInProgress2TransactionResponseMapper mapper;
    private final AuditUtilities auditUtilities;

    public CommonCaptureServiceImpl(TransactionInProgressRepository repository,
                                    TransactionInProgress2TransactionResponseMapper mapper,
                                    AuditUtilities auditUtilities) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
    }

    public TransactionResponse capturePayment(String trxCode) {
        try {
            TransactionInProgress trx = repository.findByTrxCode(trxCode)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,trx.getStatus()));
            }

            trx.setStatus(SyncTrxStatus.CAPTURED);
            trx.setElaborationDateTime(LocalDateTime.now());
            repository.save(trx);

            auditUtilities.logCapturePayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

}
