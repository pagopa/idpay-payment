package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.qrcode.QRCodeAuthPaymentService;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;
  private final QRCodeConfirmationService qrCodeConfirmationService;

  public QRCodePaymentServiceImpl(QRCodeCreationService qrCodeCreationService,
      QRCodeAuthPaymentService qrCodeAuthPaymentService, QRCodeConfirmationService qrCodeConfirmationService) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
    this.qrCodeConfirmationService = qrCodeConfirmationService;
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
  public TransactionResponse confirmPayment(String trxId, String merchantId) {
    return qrCodeConfirmationService.confirmPayment(trxId, merchantId);
  }

}
