package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface QRCodePreAuthService {

  AuthPaymentDTO relateUser(String trxCode, String userId);
}
