package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;

public interface QRCodePaymentService {

  TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest);

  AuthPaymentDTO authPayment(String userId, String trxCode);
}
