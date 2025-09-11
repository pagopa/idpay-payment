package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;

import java.util.Map;

public interface BarCodePaymentService {

    TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String userId);

    AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePayment, String merchantId, String acquirerId);

    PreviewPaymentDTO previewPayment(Map<String, String> additionalProperties, String trxCode, Long amountCents);
}
