package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/bar-code")
public interface BarCodePaymentController {

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionBarCodeResponse createTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );

    @PutMapping("/{trxCode}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(
            @PathVariable("trxCode") String trxCode,
            @RequestBody  @Valid AuthBarCodePaymentDTO authBarCodePaymentDTO,
            @RequestHeader("x-merchant-id") String merchantId
    );

}
