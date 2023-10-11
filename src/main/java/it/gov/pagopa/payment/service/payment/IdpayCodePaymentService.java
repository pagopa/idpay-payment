package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;

public interface IdpayCodePaymentService { //TODO after refactor args
  RelateUserResponse relateUser(String trxId, RelateUserRequest request);
  AuthPaymentDTO previewPayment(String trxId, String userId);
  AuthPaymentDTO authPayment(String userId, String trxId);

}
