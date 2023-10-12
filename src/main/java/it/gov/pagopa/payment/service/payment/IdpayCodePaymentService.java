package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;

public interface IdpayCodePaymentService {
  RelateUserResponse relateUser(String trxId, String fiscalCode);
  AuthPaymentDTO previewPayment(String trxId, String merchantId);
  AuthPaymentDTO authPayment(String userId, String trxId);

}
