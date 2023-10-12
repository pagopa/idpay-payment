package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/idpay-code")
public interface IdPayCodePaymentController {

    @PutMapping("/{transactionId}/relate-user")
    @ResponseStatus(code = HttpStatus.OK)
    RelateUserResponse relateUser(@PathVariable("transactionId") String trxId,
                                  @RequestHeader("Fiscal-Code") String fiscalCode);

    @PutMapping("/{trxId}/preview") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO previewPayment(@PathVariable("trxId") String trxId,
                              @RequestHeader("x-user-id") String userId);

    @PutMapping("/{trxId}/authorize") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("trxId") String trxId,
                               @RequestHeader("x-user-id") String userId);
}
