package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {

    @PutMapping("/{trxId}/relate-user") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO relateUser(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{trxId}/preview") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{trxId}/authorize") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("trxId") String trxCode,
                               @RequestHeader("x-user-id") String userId);
}
