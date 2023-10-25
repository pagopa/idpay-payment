package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodePaymentService {

  TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest, String merchantId,
      String acquirerId, String idTrxIssuer);
  AuthPaymentDTO relateUser(String trxCode, String userId);
  AuthPaymentDTO authPayment(String userId, String trxCode);

  void cancelPayment(String trxId, String merchantId, String acquirerId);
  void unrelateUser(String trxCode, String userId);
}
