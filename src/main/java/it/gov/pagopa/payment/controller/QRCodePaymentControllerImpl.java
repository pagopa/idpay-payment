package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QRCodePaymentControllerImpl implements
    QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService) {
    this.qrCodePaymentService = qrCodePaymentService;
  }

  @Override
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {
    return qrCodePaymentService.createTransaction(trxCreationRequest);
  }
}
