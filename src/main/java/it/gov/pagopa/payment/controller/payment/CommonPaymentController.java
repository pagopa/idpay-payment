package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment")
public interface CommonPaymentController {
    @PostMapping("/")
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionResponse createTransaction(
            @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId,
            @RequestHeader("x-apim-request-id") String idTrxIssuer);

    @DeleteMapping("/{transactionId}")
    @ResponseStatus(code = HttpStatus.OK)
    void cancelTransaction(
            @PathVariable("transactionId") String transactionId,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId);


    @GetMapping("/{transactionId}/status")
    @ResponseStatus(code = HttpStatus.OK)
    SyncTrxStatusDTO getStatusTransaction(@PathVariable("transactionId") String transactionId,
                                          @RequestHeader("x-merchant-id") String merchantId,
                                          @RequestHeader("x-acquirer-id") String acquirerId);

}
