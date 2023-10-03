package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePaymentServiceImpl implements IdpayCodePaymentService{
    @Override
    public AuthPaymentDTO relateUser(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String userId, String trxId) {
        return null; //TODO after refactor impl
    }
}
