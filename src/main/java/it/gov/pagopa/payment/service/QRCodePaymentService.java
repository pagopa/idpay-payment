package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodePaymentService {

  TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest, String merchantId,
      String acquirerId, String idTrxAcquirer);
  AuthPaymentDTO relateUser(String trxCode, String userId);
  AuthPaymentDTO authPayment(String userId, String trxCode);
  TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId);
  SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId, String acquirerId);
  void cancelPayment(String trxId, String merchantId, String acquirerId);
  void unrelateUserPayment(String trxCode, String userId);
}
