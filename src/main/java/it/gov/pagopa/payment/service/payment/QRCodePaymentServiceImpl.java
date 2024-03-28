package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.service.payment.qrcode.*;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodePreAuthService qrCodePreAuthService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeUnrelateService qrCodeUnrelateService;

  public QRCodePaymentServiceImpl(
          QRCodePreAuthService qrCodePreAuthService,
          QRCodeAuthPaymentService qrCodeAuthPaymentService,
          QRCodeUnrelateService qrCodeUnrelateService) {
    this.qrCodePreAuthService = qrCodePreAuthService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
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
  public void unrelateUser(String trxCode, String userId) {
    qrCodeUnrelateService.unrelateTransaction(trxCode, userId);
  }
}