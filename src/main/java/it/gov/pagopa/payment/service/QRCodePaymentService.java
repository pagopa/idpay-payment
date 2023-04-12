package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodePaymentService {

  TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest, String merchantId);

  AuthPaymentDTO authPayment(String userId, String trxCode);

  TransactionResponse relateUser(String userId, String trxCode);
}
