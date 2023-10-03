package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
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
      @RequestHeader("x-merchant-id") String merchantId,
      @RequestHeader("x-acquirer-id") String acquirerId,
      @RequestHeader("x-apim-request-id") String idTrxIssuer);

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

  @PutMapping("/merchant/{transactionId}/confirm")
  TransactionResponse confirmPayment(@PathVariable("transactionId") String trxId, @RequestHeader("x-merchant-id") String merchantId, @RequestHeader("x-acquirer-id") String acquirerId);

  @DeleteMapping("/merchant/{transactionId}")
  @ResponseStatus(code = HttpStatus.OK)
  void cancelTransaction(@PathVariable("transactionId") String transactionId, @RequestHeader("x-merchant-id") String merchantId, @RequestHeader("x-acquirer-id") String acquirerId);

  @GetMapping("/merchant/status/{transactionId}")
  @ResponseStatus(code = HttpStatus.OK)
  SyncTrxStatusDTO getStatusTransaction(@PathVariable("transactionId") String transactionId, @RequestHeader("x-merchant-id") String merchantId, @RequestHeader("x-acquirer-id") String acquirerId);

  @PutMapping("/force-expiration/confirm/{initiativeId}")
  Long forceConfirmTrxExpiration(@PathVariable("initiativeId") String initiativeId);

  @PutMapping("/force-expiration/authorization/{initiativeId}")
  Long forceAuthorizationTrxExpiration(@PathVariable("initiativeId") String initiativeId);

}