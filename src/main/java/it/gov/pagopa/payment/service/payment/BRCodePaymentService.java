package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface BRCodePaymentService { //TODO after refactor args

  TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest, String merchantId,
      String acquirerId, String idTrxIssuer);
  AuthPaymentDTO authPayment(String userId, String trxCode);

}
