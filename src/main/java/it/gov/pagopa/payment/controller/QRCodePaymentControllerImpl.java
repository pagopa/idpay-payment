package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.service.qrCodeAuth.QRCodeAuthPaymentService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QRCodePaymentControllerImpl implements
    QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;
  private final QRCodeAuthPaymentService qrCodeAuthPaymentService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService,
      QRCodeAuthPaymentService qrCodeAuthPaymentService) {
    this.qrCodePaymentService = qrCodePaymentService;
    this.qrCodeAuthPaymentService = qrCodeAuthPaymentService;
  }

  @Override
  @PerformanceLog("CREATE_TRANSACTION_QR_CODE")
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {
    return qrCodePaymentService.createTransaction(trxCreationRequest);
  }

  @Override
  @PerformanceLog("AUTHORIZE_TRANSACTION_QR_CODE")
  public AuthPaymentDTO authPayment(String trxCode, String userId) {
    return qrCodeAuthPaymentService.authPayment(userId, trxCode);
  }
}
