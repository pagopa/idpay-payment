package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;

public interface IdpayCodeAuthPaymentService {
    AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody);
}
