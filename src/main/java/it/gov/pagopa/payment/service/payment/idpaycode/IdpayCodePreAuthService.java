package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;

public interface IdpayCodePreAuthService {

  RelateUserResponse relateUser(String trxId, String fiscalCode);
  AuthPaymentDTO previewPayment(String trxId, String merchantId);
}
