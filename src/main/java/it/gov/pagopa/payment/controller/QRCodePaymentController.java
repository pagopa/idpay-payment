package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/qr-code/merchant")
public interface QRCodePaymentController {

  @PostMapping("/")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(
      @RequestBody TransactionCreationRequest trxCreationRequest,
      @RequestHeader("x-merchant-id") String merchantId);

  @PutMapping("/{trxCode}/authorize/{userId}")
  @ResponseStatus(code = HttpStatus.OK)
  AuthPaymentDTO authPayment(@PathVariable("trxCode") String trxCode,
      @RequestHeader("x-userId-id") String userId);
}
