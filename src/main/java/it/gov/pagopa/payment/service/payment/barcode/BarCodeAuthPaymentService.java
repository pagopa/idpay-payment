package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String trxCode, String merchantId, long amountCents);
}
