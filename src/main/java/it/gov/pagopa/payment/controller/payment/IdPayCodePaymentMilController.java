package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/mil/payment/idpay-code")
public interface IdPayCodePaymentMilController {

    @PutMapping("/{transactionId}/preview")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("transactionId") String trxId,
                                  @RequestHeader("x-merchant-id") String merchantId);

    @PutMapping("/{trxId}/authorize") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("trxId") String trxId,
                               @RequestHeader("x-user-id") String userId);
}
