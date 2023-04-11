package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/qr-code/merchant")
public interface QRCodePaymentController {

  @PostMapping("/")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(@RequestBody TransactionCreationRequest trxCreationRequest);
}
