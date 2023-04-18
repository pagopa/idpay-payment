package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatus;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import it.gov.pagopa.payment.service.qrcode.QRCodePreAuthService;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodePreAuthService qrCodePreAuthService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeConfirmationService qrCodeConfirmationService;
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final TransactionInProgress2SyncTrxStatus transactionMapper;

  public QRCodePaymentServiceImpl(
          QRCodeCreationService qrCodeCreationService,
          QRCodePreAuthService qrCodePreAuthService,
          QRCodeAuthPaymentService qrCodeAuthPaymentService,
          QRCodeConfirmationService qrCodeConfirmationService, TransactionInProgressRepository transactionInProgressRepository, TransactionInProgress2SyncTrxStatus transactionMapper) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodePreAuthService = qrCodePreAuthService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodeConfirmationService = qrCodeConfirmationService;
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.transactionMapper = transactionMapper;
  }

  @Override
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest, String merchantId) {
    return qrCodeCreationService.createTransaction(trxCreationRequest, RewardConstants.TRX_CHANNEL_QRCODE, merchantId);
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    return qrCodePreAuthService.relateUser(trxCode, userId);
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    return qrCodeAuthPaymentService.authPayment(userId,trxCode);
  }


  @Override
  public TransactionResponse confirmPayment(String trxId, String merchantId) {
    return qrCodeConfirmationService.confirmPayment(trxId, merchantId);
  }

  @Override
  public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId, String acquirerId) {
    TransactionInProgress transactionInProgress= transactionInProgressRepository.findByIdAndMerchantIdAndAcquirerId(transactionId, merchantId, acquirerId)
            .orElseThrow(() -> new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Transaction does not exist"));

    return transactionMapper.transactionInProgressMapper(transactionInProgress);
  }

}
