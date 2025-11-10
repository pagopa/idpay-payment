package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/idpay/payment")
public interface CommonPaymentController {
    @PostMapping("/")
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionResponse createTransaction(
            @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId,
            @RequestHeader("x-apim-request-id") String idTrxIssuer);

    @PutMapping("/{transactionId}/confirm")
    TransactionResponse confirmPayment(
            @PathVariable("transactionId") String trxId,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId);

    @DeleteMapping("/transactions/{transactionId}")
    @ResponseStatus(code = HttpStatus.OK)
    void cancelTransaction(
            @PathVariable("transactionId") String transactionId,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId,
            @RequestHeader("x-point-of-sale-id") String pointOfSaleId);

    @DeleteMapping("/pendingTransactions")
    @ResponseStatus(code = HttpStatus.OK)
    void cancelPendingTransactions();

    @PostMapping("/transactions/{transactionId}/reversal")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void reversalTransaction(
        @PathVariable("transactionId") String transactionId,
        @RequestHeader("x-merchant-id") String merchantId,
        @RequestHeader("x-point-of-sale-id") String pointOfSaleId,
        @RequestPart("file") MultipartFile file,
        @RequestPart("creditNoteNumber") String creditNoteNumber
    );

    @PostMapping("/transactions/{transactionId}/invoice")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void invoiceTransaction(
        @PathVariable("transactionId") String transactionId,
        @RequestHeader("x-merchant-id") String merchantId,
        @RequestHeader("x-point-of-sale-id") String pointOfSaleId,
        @RequestPart("file") MultipartFile file,
        @RequestPart("invoiceNumber") String invoiceNumber
    );

    @GetMapping("/{transactionId}/status")
    @ResponseStatus(code = HttpStatus.OK)
    SyncTrxStatusDTO getStatusTransaction(@PathVariable("transactionId") String transactionId,
                                          @RequestHeader("x-merchant-id") String merchantId);

    @PutMapping("/force-expiration/confirm/{initiativeId}")
    Long forceConfirmTrxExpiration(@PathVariable("initiativeId") String initiativeId);

    @PutMapping("/force-expiration/authorization/{initiativeId}")
    Long forceAuthorizationTrxExpiration(@PathVariable("initiativeId") String initiativeId);

}
