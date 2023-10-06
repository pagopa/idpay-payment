package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/br-code")
public interface BRCodePaymentController {

    @PostMapping("/citizen") //TODO after refactor path
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionBRCodeResponse createTransaction(
            @RequestBody @Valid TransactionBRCodeCreationRequest trxBRCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );

    @PutMapping("/{trxCode}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(
            @PathVariable("trxCode") String trxCode,
            @RequestHeader("x-merchant-id") String merchantId
    );

}
