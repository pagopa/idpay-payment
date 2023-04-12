package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodePreAuthService {

  TransactionResponse relateUser(String trxCode, String userId);
}
