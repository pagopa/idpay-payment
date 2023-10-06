package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BarCodePaymentServiceImpl implements BarCodePaymentService {

    private final BarCodeAuthPaymentService barCodeAuthPaymentService;

    public BarCodePaymentServiceImpl(BarCodeAuthPaymentService brCodeAuthPaymentService){
        this.barCodeAuthPaymentService = brCodeAuthPaymentService;
    }

    @Override
    public TransactionBRCodeResponse createTransaction(TransactionBRCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, long amountCents, String merchantId) {
        return barCodeAuthPaymentService.authPayment(null, trxCode, merchantId, amountCents);
    }
}
