package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodePreviewService;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeRelateUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdpayCodePaymentServiceImpl implements IdpayCodePaymentService{
    private final IdpayCodeRelateUserService idpayCodeRelateUserService;
    private final IdpayCodePreviewService idpayCodePreviewService;
    private final IdpayCodeAuthPaymentService idpayCodeAuthPaymentService;

    public IdpayCodePaymentServiceImpl(IdpayCodeRelateUserService idpayCodeRelateUserService,
                                       IdpayCodePreviewService idpayCodePreviewService,
                                       IdpayCodeAuthPaymentService idpayCodeAuthPaymentService) {
        this.idpayCodeRelateUserService = idpayCodeRelateUserService;
        this.idpayCodePreviewService = idpayCodePreviewService;
        this.idpayCodeAuthPaymentService = idpayCodeAuthPaymentService;
    }

    @Override
    public RelateUserResponse relateUser(String trxId, String fiscalCode) {
        return idpayCodeRelateUserService.relateUser(trxId, fiscalCode);
    }

    @Override
    public AuthPaymentDTO previewPayment(String trxId, String merchantId) {
        return idpayCodePreviewService.previewPayment(trxId, merchantId);
    }

    @Override
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody) {
        return idpayCodeAuthPaymentService.authPayment(trxId, merchantId, pinBlockBody);
    }
}
