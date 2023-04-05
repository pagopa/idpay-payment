package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;

public interface QRCodePaymentService {

  TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest);
}
