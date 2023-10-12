package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;

public interface IdpayCodePreAuthService {

  RelateUserResponse relateUser(String trxId, String fiscalCode);
}
