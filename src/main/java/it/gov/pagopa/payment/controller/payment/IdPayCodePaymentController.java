package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {

    @PutMapping("/{transactionId}/relate-user") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO relateUser(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{transactionId}/preview") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{transactionId}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("transactionId") String trxId,
                               @RequestHeader("x-acquirer-id") String acquirerId,
                               @RequestHeader("x-merchant-fiscal-code") String merchantFiscalCode,
                               @RequestBody PinBlockDTO pinBlockbody);
}
