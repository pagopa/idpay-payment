package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/qr-code")
public interface QRCodePaymentController {

  @PostMapping("/merchant")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(
      @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
      @RequestHeader("x-merchant-id") String merchantId,
      @RequestHeader("x-acquirer-id") String acquirerId,
      @RequestHeader("x-apim-request-id") String idTrxAcquirer);

  @PutMapping("/{trxCode}/relate-user")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO relateUser(@PathVariable("trxCode") String trxCode,
                                 @RequestHeader("x-user-id") String userId);

  @PutMapping("/{trxCode}/authorize")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO authPayment(@PathVariable("trxCode") String trxCode,
      @RequestHeader("x-user-id") String userId);

  @PutMapping("/merchant/{transactionId}/confirm")
  TransactionResponse confirmPayment(@PathVariable("transactionId") String trxId, @RequestHeader("x-merchant-id") String merchantId, @RequestHeader("x-acquirer-id") String acquirerId);
}
