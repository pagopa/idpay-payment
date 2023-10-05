package it.gov.pagopa.payment.service.payment.brcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String userId, String trxCode, String merchantId);
}
