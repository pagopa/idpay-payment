package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/qr-code")
public interface QRCodePaymentController {

  @PostMapping("/merchant")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(
      @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
      @RequestHeader("x-merchant-id") String merchantId);

  @PutMapping("/{trxCode}/relate-user")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO relateUser(@PathVariable("trxCode") String trxCode,
                                 @RequestHeader("x-user-id") String userId);

  @PutMapping("/{trxCode}/authorize")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO authPayment(@PathVariable("trxCode") String trxCode,
      @RequestHeader("x-user-id") String userId);

  @PutMapping("/merchant/{transactionId}/confirm")
  TransactionResponse confirmPayment(@PathVariable("transactionId") String trxId, @RequestHeader("x-merchant-id") String merchantId);
}
