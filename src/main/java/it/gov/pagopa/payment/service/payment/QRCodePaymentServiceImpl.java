package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.qrcode.*;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodePreAuthService qrCodePreAuthService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeConfirmationService qrCodeConfirmationService;
  private final QRCodeCancelService qrCodeCancelService;
  private final QRCodeUnrelateService qrCodeUnrelateService;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper;

  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public QRCodePaymentServiceImpl(
          QRCodeCreationService qrCodeCreationService,
          QRCodePreAuthService qrCodePreAuthService,
          QRCodeAuthPaymentService qrCodeAuthPaymentService,
          QRCodeConfirmationService qrCodeConfirmationService,
          QRCodeCancelService qrCodeCancelService,
          QRCodeUnrelateService qrCodeUnrelateService,
          TransactionInProgressRepository transactionInProgressRepository,
          TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodePreAuthService = qrCodePreAuthService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodeConfirmationService = qrCodeConfirmationService;
    this.qrCodeCancelService = qrCodeCancelService;
    this.qrCodeUnrelateService = qrCodeUnrelateService;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.transaction2statusMapper = transaction2statusMapper;
  }

  @Override
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest,
      String merchantId,
      String acquirerId,
      String idTrxIssuer) {
    return qrCodeCreationService.createQRCodeTransaction(
        trxCreationRequest,
        RewardConstants.TRX_CHANNEL_QRCODE,
        merchantId,
        acquirerId,
        idTrxIssuer);
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    return qrCodePreAuthService.relateUser(trxCode, userId);
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    return qrCodeAuthPaymentService.authPayment(userId, trxCode);
  }

  @Override
  public TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId) {
    return qrCodeConfirmationService.confirmPayment(trxId, merchantId, acquirerId);
  }

  @Override
  public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId, String acquirerId) {
    TransactionInProgress transactionInProgress= transactionInProgressRepository.findByIdAndMerchantIdAndAcquirerId(transactionId, merchantId, acquirerId)
            .orElseThrow(() -> new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

    return transaction2statusMapper.transactionInProgressMapper(transactionInProgress);
  }

  @Override
  public void cancelPayment(String trxId, String merchantId, String acquirerId) {
    qrCodeCancelService.cancelTransaction(trxId, merchantId, acquirerId);
  }

  @Override
  public void unrelateUser(String trxCode, String userId) {
    qrCodeUnrelateService.unrelateTransaction(trxCode, userId);
  }
}