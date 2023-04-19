package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class QRCodePaymentControllerImpl implements QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService) {
    this.qrCodePaymentService = qrCodePaymentService;
  }

  @Override
  @PerformanceLog(
      value = "QR_CODE_CREATE_TRANSACTION",
      payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
  public TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest,
      String merchantId,
      String acquirerId,
      String idTrxAcquirer) {
    log.info("[QR_CODE_CREATE_TRANSACTION] The merchant {} is creating a transaction", merchantId);
    return qrCodePaymentService.createTransaction(trxCreationRequest, merchantId, acquirerId, idTrxAcquirer);
  }

  @Override
  @PerformanceLog(
      value = "QR_CODE_RELATE_USER",
      payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    log.info(
        "[QR_CODE_RELATE_USER] The user {} is trying to relate to transaction having trxCode {}",
        userId,
        trxCode);
    return qrCodePaymentService.relateUser(trxCode, userId);
  }

  @Override
  @PerformanceLog(
      value = "QR_CODE_AUTHORIZE_TRANSACTION",
      payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
  public AuthPaymentDTO authPayment(String trxCode, String userId) {
    log.info(
        "[QR_CODE_AUTHORIZE_TRANSACTION] The user {} is authorizing the transaction having trxCode {}",
        userId,
        trxCode);
    return qrCodePaymentService.authPayment(userId, trxCode);
  }

  @Override
  @PerformanceLog(
      value = "QR_CODE_CONFIRM_PAYMENT",
      payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
  public TransactionResponse confirmPayment(String trxId, String merchantId) {
    log.info(
        "[QR_CODE_CONFIRM_PAYMENT] The merchant {} is confirming the transaction {}",
        merchantId,
        trxId);
    return qrCodePaymentService.confirmPayment(trxId, merchantId);
  }
}
