package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;

public interface IdpayCodePaymentService { //TODO after refactor args
  UserRelateResponse relateUser(String trxId, String userId);
  AuthPaymentDTO previewPayment(String trxId, String userId);
  AuthPaymentDTO authPayment(String userId, String trxId);

}
