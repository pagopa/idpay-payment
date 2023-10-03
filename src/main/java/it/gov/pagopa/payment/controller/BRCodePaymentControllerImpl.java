package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BRCodePaymentControllerImpl implements BRCodePaymentController{
    @Override
    public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest, String acquirerId, String idTrxIssuer) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, String userId) {
        return null; //TODO after refactor impl
    }
}
