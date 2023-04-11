package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QRCodePaymentControllerImpl implements
    QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService) {
    this.qrCodePaymentService = qrCodePaymentService;
  }

  @Override
  @PerformanceLog("CREATE_TRANSACTION_QR_CODE")
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {
    return qrCodePaymentService.createTransaction(trxCreationRequest);
  }

  @Override
  public ResponseEntity<AuthPaymentDTO> authPayment(String trxCode, String userId) {
    AuthPaymentDTO authPaymentDTO = qrCodePaymentService.authPayment(userId, trxCode);
    return new ResponseEntity<>(authPaymentDTO, HttpStatus.OK);
  }
}
