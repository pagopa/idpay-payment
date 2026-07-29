package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;

public interface BarCodeCaptureService {
    TransactionBarCodeResponse capturePayment(String initiativeId, String trxCode, String merchantId, String pointOfSaleId, String acquirerId);

    TransactionBarCodeResponse retriveVoucher(String intiativeId, String trxCode, String userId);
}
