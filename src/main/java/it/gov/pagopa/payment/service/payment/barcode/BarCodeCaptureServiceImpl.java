package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class BarCodeCaptureServiceImpl implements BarCodeCaptureService {

    private final TransactionInProgressRepository repository;
    private final TransactionBarCodeInProgress2TransactionResponseMapper mapper;
    private final AuditUtilities auditUtilities;

    public BarCodeCaptureServiceImpl(TransactionInProgressRepository repository,
                                     TransactionBarCodeInProgress2TransactionResponseMapper mapper,
                                    AuditUtilities auditUtilities) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditUtilities = auditUtilities;
    }

    public TransactionBarCodeResponse capturePayment(String trxCode) {
        try {
            TransactionInProgress trx = repository.findByTrxCode(trxCode.toLowerCase())
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,trx.getStatus()));
            }

            deleteUnusedVouchers(trx);

            trx.setStatus(SyncTrxStatus.CAPTURED);
            trx.setElaborationDateTime(LocalDateTime.now());
            trx.setUpdateDate(LocalDateTime.now());
            repository.save(trx);

            auditUtilities.logCapturePayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }

    @Override
    public TransactionBarCodeResponse retriveVoucher(String intiativeId, String trxCode, String userId) {
        try {
            TransactionInProgress trx = repository.findByInitiativeIdAndTrxCodeAndUserId(intiativeId, trxCode, userId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find voucher with transactionCode [%s]".formatted(trxCode)));

            auditUtilities.logRetriveVoucher(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorRetriveVoucher(intiativeId, trxCode, userId);
            throw e;
        }
    }

    private void deleteUnusedVouchers(TransactionInProgress trx) {
        List<TransactionInProgress> otherTrxs = repository
            .findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                trx.getUserId(),
                trx.getInitiativeId(),
                SyncTrxStatus.CREATED,
                trx.getExtendedAuthorization()
            );

        if (!otherTrxs.isEmpty()) {
            repository.deleteAll(otherTrxs);
            otherTrxs.forEach(otherTrx ->
                log.info("[CAPTURE_PAYMENT] Removed unused {} voucher (id={}) for user={} initiative={}",
                    Boolean.TRUE.equals(otherTrx.getExtendedAuthorization()) ? "WEB" : "APP",
                    otherTrx.getId(),
                    trx.getUserId(),
                    trx.getInitiativeId())
            );
        }
    }
}
