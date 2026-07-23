package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;

public interface IdpayCodePreviewService {
    AuthPaymentDTO previewPayment(String trxId, String merchantId);
}
