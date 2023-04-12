package it.gov.pagopa.payment.service.qrCodeAuth;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;

public interface QRCodeAuthPaymentService {
  AuthPaymentDTO authPayment(String userId, String trxCode);

}
