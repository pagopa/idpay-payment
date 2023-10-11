package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;

public interface IdpayCodePreAuthService {

  RelateUserResponse relateUser(String trxId, RelateUserRequest request);

  AuthPaymentDTO previewPayment(String trxId, String merchantId);
}
