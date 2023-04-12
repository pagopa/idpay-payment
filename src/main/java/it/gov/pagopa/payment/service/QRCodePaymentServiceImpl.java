package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.qrcode.QRCodeCreationService;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements QRCodePaymentService {

  private final QRCodeCreationService qrCodeCreationService;

  public QRCodePaymentServiceImpl(QRCodeCreationService qrCodeCreationService) {
    this.qrCodeCreationService = qrCodeCreationService;
  }

  @Override
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest, String merchantId) {
    return qrCodeCreationService.createTransaction(trxCreationRequest, merchantId);
  }
}
