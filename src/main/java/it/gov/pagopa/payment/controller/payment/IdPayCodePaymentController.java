package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateRequest;
import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {

    @PutMapping("/{transactionId}/minint/relate-user")
    @ResponseStatus(code = HttpStatus.OK)
    UserRelateResponse relateUser(@PathVariable("transactionId") String trxId,
                                  @RequestBody UserRelateRequest request);

    @PutMapping("/{trxId}/preview") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{trxId}/authorize") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("trxId") String trxId,
                               @RequestHeader("x-user-id") String userId);
}
