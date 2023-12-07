package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/qr-code")
public interface QRCodePaymentController {

  @PutMapping("/{trxCode}/relate-user")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO relateUser(@PathVariable("trxCode") String trxCode,
                                 @RequestHeader("x-user-id") String userId);

  @PutMapping("/{trxCode}/authorize")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO authPayment(@PathVariable("trxCode") String trxCode,
      @RequestHeader("x-user-id") String userId);

  @DeleteMapping("/{trxCode}")
  @ResponseStatus(code = HttpStatus.OK)
  void unrelateUser(@PathVariable("trxCode") String trxCode,
                    @RequestHeader("x-user-id") String userId);

}