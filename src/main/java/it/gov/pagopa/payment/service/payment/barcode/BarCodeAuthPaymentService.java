package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;

import java.util.Map;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String acquirerId);

    PreviewPaymentDTO previewPayment(Map<String, String> additionalProperties, String trxCode, Long amountCents);

}
