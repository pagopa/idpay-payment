package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;

public interface QRCodePaymentService {

  TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest);
}
