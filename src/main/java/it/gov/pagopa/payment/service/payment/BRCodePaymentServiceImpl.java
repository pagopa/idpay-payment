package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BRCodePaymentServiceImpl implements BRCodePaymentService{
    @Override
    public TransactionResponse createTransaction(TransactionCreationRequest trxCreationRequest, String merchantId, String acquirerId, String idTrxIssuer) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String userId, String trxCode) {
        return null; //TODO after refactor impl
    }
}
