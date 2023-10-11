package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {

    @PutMapping("/{transactionId}/relate-user")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO relateUser(@PathVariable("transactionId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{transactionId}/preview")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("transactionId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{transactionId}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("transactionId") String trxId,
                               @RequestHeader("x-merchant-id") String merchantId,
                               @Valid @RequestBody PinBlockDTO pinBlockbody);
}
