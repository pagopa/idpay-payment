package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodePreAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePaymentServiceImpl implements IdpayCodePaymentService{
    private final IdpayCodePreAuthService idpayCodePreAuthService;

    public IdpayCodePaymentServiceImpl(IdpayCodePreAuthService idpayCodePreAuthService) {
        this.idpayCodePreAuthService = idpayCodePreAuthService;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        return idpayCodePreAuthService.relateUser(trxId, request);
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String acquirerId, String merchantFiscalCode) {
        return idpayCodePreAuthService.previewPayment(trxId, acquirerId, merchantFiscalCode);
    }

    @Override
    public AuthPaymentDTO authPayment(String userId, String trxId) {
        return null; //TODO after refactor impl
    }
}
