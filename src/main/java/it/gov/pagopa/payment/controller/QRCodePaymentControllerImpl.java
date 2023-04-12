package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class QRCodePaymentControllerImpl implements
    QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService) {
    this.qrCodePaymentService = qrCodePaymentService;
  }

  @Override
  @PerformanceLog(value = "CREATE_TRANSACTION_QR_CODE", payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
  public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest) {
    return qrCodePaymentService.createTransaction(trxCreationRequest);
  }

  @Override
  @PerformanceLog(value = "QR_CODE_CONFIRM_PAYMENT", payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
  public TransactionResponse confirmPayment(String trxId, String merchantId) {
    log.info("[QR_CODE_CONFIRM_PAYMENT] The merchant {} is confirming the transaction {}", merchantId, trxId);
    return qrCodePaymentService.confirmPayment(trxId, merchantId);
  }
}
