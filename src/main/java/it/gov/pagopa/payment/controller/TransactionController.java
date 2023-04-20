package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/transaction")
public interface TransactionController {
    @GetMapping("/{transactionId}")
    @ResponseStatus(code = HttpStatus.OK)
    TransactionInProgress getTransaction(@PathVariable String transactionId, @RequestHeader("x-user-id") String userId);
}
