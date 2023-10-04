package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface IdpayCodePaymentService { //TODO after refactor args
  AuthPaymentDTO relateUser(String trxId, String userId);
  AuthPaymentDTO previewPayment(String trxId, String userId);
  AuthPaymentDTO authPayment(String userId, String trxId);

}
