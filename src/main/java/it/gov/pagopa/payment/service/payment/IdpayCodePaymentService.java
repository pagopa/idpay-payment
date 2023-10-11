package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;

public interface IdpayCodePaymentService {
  AuthPaymentDTO relateUser(String trxId, String userId);
  AuthPaymentDTO previewPayment(String trxId, String userId);
  AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody);

}
