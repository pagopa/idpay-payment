package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.service.payment.brcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.brcode.BarCodeAuthPaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BRCodePaymentServiceImpl implements BRCodePaymentService {

    private final BarCodeAuthPaymentService barCodeAuthPaymentService;

    public BRCodePaymentServiceImpl(BarCodeAuthPaymentServiceImpl brCodeAuthPaymentService){
        this.barCodeAuthPaymentService = brCodeAuthPaymentService;
    }

    @Override
    public TransactionBRCodeResponse createTransaction(TransactionBRCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, String merchantId) {
        return barCodeAuthPaymentService.authPayment(null, trxCode, merchantId);
    }
}
