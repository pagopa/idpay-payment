package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.UserRelateRequest;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;

public interface IdpayCodePreAuthService {

  UserRelateResponse relateUser(String trxId, UserRelateRequest request);
}
