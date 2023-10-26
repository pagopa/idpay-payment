package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.qrcode.*;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodePreAuthService qrCodePreAuthService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeUnrelateService qrCodeUnrelateService;

  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public QRCodePaymentServiceImpl(
          QRCodeCreationService qrCodeCreationService,
          QRCodePreAuthService qrCodePreAuthService,
          QRCodeAuthPaymentService qrCodeAuthPaymentService,
          QRCodeUnrelateService qrCodeUnrelateService) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodePreAuthService = qrCodePreAuthService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodeUnrelateService = qrCodeUnrelateService;
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
  public void unrelateUser(String trxCode, String userId) {
    qrCodeUnrelateService.unrelateTransaction(trxCode, userId);
  }
}