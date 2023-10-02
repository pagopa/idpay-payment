package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface QRCodeAuthPaymentService {
  AuthPaymentDTO authPayment(String userId, String trxCode);

}
