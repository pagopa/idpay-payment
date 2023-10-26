package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.qrcode.*;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodePreAuthService qrCodePreAuthService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeCancelService qrCodeCancelService;
  private final QRCodeUnrelateService qrCodeUnrelateService;

  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public QRCodePaymentServiceImpl(
          QRCodePreAuthService qrCodePreAuthService,
          QRCodeAuthPaymentService qrCodeAuthPaymentService,
          QRCodeCancelService qrCodeCancelService,
          QRCodeUnrelateService qrCodeUnrelateService) {
    this.qrCodePreAuthService = qrCodePreAuthService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodeCancelService = qrCodeCancelService;
    this.qrCodeUnrelateService = qrCodeUnrelateService;
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
  public void cancelPayment(String trxId, String merchantId, String acquirerId) {
    qrCodeCancelService.cancelTransaction(trxId, merchantId, acquirerId);
  }

  @Override
  public void unrelateUser(String trxCode, String userId) {
    qrCodeUnrelateService.unrelateTransaction(trxCode, userId);
  }
}