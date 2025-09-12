package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;

public interface BarCodeAuthPaymentService {

    AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId);

    PreviewPaymentDTO previewPayment(String trxCode, Long amountCents);

}
