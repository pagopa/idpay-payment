package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/qr-code")
public interface QRCodePaymentController {

  @PostMapping("/")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(
          @RequestBody TransactionCreationRequest trxCreationRequest,
          @RequestHeader("x-merchant-id") String merchantId);

}
