package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeAuthPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePaymentServiceImpl implements IdpayCodePaymentService{
    private final IdpayCodeAuthPaymentService idpayCodeAuthPaymentService;

    public IdpayCodePaymentServiceImpl(IdpayCodeAuthPaymentService idpayCodeAuthPaymentService) {
        this.idpayCodeAuthPaymentService = idpayCodeAuthPaymentService;
    }

    @Override
    public AuthPaymentDTO relateUser(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody) {
        return idpayCodeAuthPaymentService.authPayment(trxId, merchantId, pinBlockBody);
    }
}
