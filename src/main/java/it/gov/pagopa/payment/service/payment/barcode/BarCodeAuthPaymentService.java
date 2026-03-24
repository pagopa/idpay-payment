package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import java.util.Map;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId);

    PreviewPaymentResultDTO previewPayment(String trxCode, Map<String, String> additionalProperties, Long amountCents);
}
