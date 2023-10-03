package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/br-code")
public interface BRCodePaymentController {

    @PostMapping("/citizen") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionResponse createTransaction( //TODO after refactor headers
            @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
            @RequestHeader("x-acquirer-id") String acquirerId,
            @RequestHeader("x-apim-request-id") String idTrxIssuer);

    @PutMapping("/{trxId}/authorize") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(@PathVariable("trxId") String trxCode,
                               @RequestHeader("x-user-id") String userId);
}
