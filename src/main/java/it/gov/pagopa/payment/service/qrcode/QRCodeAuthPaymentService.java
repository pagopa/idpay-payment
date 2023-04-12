package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;

public interface QRCodeAuthPaymentService {
  AuthPaymentDTO authPayment(String userId, String trxCode);

}
