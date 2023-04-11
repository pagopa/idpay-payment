package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/qr-code")
public interface QRCodePaymentController {

  @PostMapping("/")
  @ResponseStatus(code = HttpStatus.CREATED)
  TransactionResponse createTransaction(@RequestBody TransactionCreationRequest trxCreationRequest);

  @PutMapping("/{trxCode}/{userId}/authorize")
  ResponseEntity<AuthPaymentDTO> authPayment(@PathVariable("trxCode") String trxCode,
      @PathVariable("userId") String userId);
}
