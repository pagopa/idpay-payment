package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import it.gov.pagopa.payment.service.qrcode.QRCodePreAuthService;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodePreAuthService qrCodePreAuthService;

  public QRCodePaymentServiceImpl(QRCodeCreationService qrCodeCreationService,
      QRCodeAuthPaymentService qrCodeAuthPaymentService, QRCodePreAuthService qrCodePreAuthService) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodePreAuthService = qrCodePreAuthService;
  }

  @Override
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest, String merchantId) {
    return qrCodeCreationService.createTransaction(trxCreationRequest, merchantId);
  }

  @Override
  public AuthPaymentDTO authPayment(String userId, String trxCode) {
    return qrCodeAuthPaymentService.authPayment(userId,trxCode);
  }

  @Override
  public TransactionResponse relateUser(String userId, String trxCode) {
    return qrCodePreAuthService.relateUser(userId, trxCode);
  }

}
