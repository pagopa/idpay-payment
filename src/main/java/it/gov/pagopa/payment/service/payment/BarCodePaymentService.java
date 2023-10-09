package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;

public interface BarCodePaymentService {

    TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String userId);
    AuthPaymentDTO authPayment(String trxCode, long amountCents, String merchantId);
}
