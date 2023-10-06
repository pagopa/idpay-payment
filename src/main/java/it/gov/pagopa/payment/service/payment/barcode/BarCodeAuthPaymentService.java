package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String userId, String trxCode, String merchantId, Long amountCents);
}
