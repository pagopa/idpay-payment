package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.service.payment.QRCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
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
          value = "QR_CODE_USER_CANCEL_TRANSACTION"
  )
  public void unrelateUser(String trxCode, String userId) {
      log.info(
             "[QR_CODE_USER_CANCEL_TRANSACTION] The user {} is unrelating the transaction having trxCode {}",
             userId,
             trxCode
      );
      qrCodePaymentService.unrelateUser(trxCode, userId);
  }
}
