package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdPayCodePaymentControllerImpl implements IdPayCodePaymentController {
    @Override
    public AuthPaymentDTO relateUser(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String userId) {
        return null; //TODO after refactor impl
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, String userId) {
        return null; //TODO after refactor impl
    }
}
