package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodePaymentService {

  TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest, String merchantId);
  TransactionResponse confirmPayment(String trxId, String merchantId);
}
