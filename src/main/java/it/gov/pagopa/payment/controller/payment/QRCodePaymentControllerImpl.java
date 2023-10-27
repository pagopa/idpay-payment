package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.QRCodePaymentService;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class QRCodePaymentControllerImpl implements QRCodePaymentController {

  private final QRCodePaymentService qrCodePaymentService;
  private final QRCodeExpirationService qrCodeExpirationService;

  public QRCodePaymentControllerImpl(QRCodePaymentService qrCodePaymentService, QRCodeExpirationService qrCodeExpirationService) {
    this.qrCodePaymentService = qrCodePaymentService;
    this.qrCodeExpirationService = qrCodeExpirationService;
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

  @Override
  @PerformanceLog("FORCE_CONFIRM_EXPIRATION")
  public Long forceConfirmTrxExpiration(String initiativeId) {
    log.info("[FORCE_CONFIRM_EXPIRATION] Requested confirm trx expiration for initiative {}", initiativeId);
    return qrCodeExpirationService.forceConfirmTrxExpiration(initiativeId);
  }

  @Override
  @PerformanceLog("FORCE_AUTHORIZATION_EXPIRATION")
  public Long forceAuthorizationTrxExpiration(String initiativeId) {
    log.info("[FORCE_AUTHORIZATION_EXPIRATION] Requested authorization trx expiration for initiative {}", initiativeId);
    return qrCodeExpirationService.forceAuthorizationTrxExpiration(initiativeId);
  }
}
