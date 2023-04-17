package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/transaction")
public interface TransactionController {
    @GetMapping("/{transactionId}")
    @ResponseStatus(code = HttpStatus.OK)
    TransactionInProgress getTransaction(@PathVariable String transactionId, @RequestHeader("x-user-id") String userId);

    @GetMapping("/status/{transactionId}")
    @ResponseStatus(code = HttpStatus.OK)
    SyncTrxStatus getStatusTransaction(@PathVariable String transactionId, @RequestHeader("x-merchant-id") String merchantId);
}
