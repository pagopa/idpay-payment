package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateRequest;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;

public interface IdpayCodePaymentService { //TODO after refactor args
  UserRelateResponse relateUser(String trxId, UserRelateRequest request);
  AuthPaymentDTO previewPayment(String trxId, String userId);
  AuthPaymentDTO authPayment(String userId, String trxId);

}
