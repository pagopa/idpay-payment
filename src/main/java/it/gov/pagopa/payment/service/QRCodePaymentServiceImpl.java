package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.qrcode.QRCodeConfirmationService;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;
  private final QRCodeConfirmationService qrCodeConfirmationService;

  public QRCodePaymentServiceImpl(QRCodeCreationService qrCodeCreationService, QRCodeConfirmationService qrCodeConfirmationService) {
    this.qrCodeCreationService = qrCodeCreationService;
    this.qrCodeConfirmationService = qrCodeConfirmationService;
  }

  @Override
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {
    return qrCodeCreationService.createTransaction(trxCreationRequest);
  }

  @Override
  public TransactionResponse confirmPayment(String trxId, String merchantId) {
    return qrCodeConfirmationService.confirmPayment(trxId, merchantId);
  }

}
