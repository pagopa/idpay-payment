package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {
    @PutMapping("/{transactionId}/relate-user")
    @ResponseStatus(code = HttpStatus.OK)
    RelateUserResponse relateUser(@PathVariable("transactionId") String trxId,
                                  @RequestHeader("Fiscal-Code") String fiscalCode);
}
