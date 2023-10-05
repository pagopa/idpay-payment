package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;

public interface BRCodePaymentService {

    TransactionBRCodeResponse createTransaction(TransactionBRCodeCreationRequest trxBRCodeCreationRequest, String userId);
    AuthPaymentDTO authPayment(String trxCode, String merchantId);
}
