package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.TransactionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/idpay/payment/transaction")
public interface TransactionController {
    @GetMapping("/{transactionId}/{userId}")
    @ResponseStatus(code = HttpStatus.OK)
    TransactionDTO getTransaction(@PathVariable String transactionId, @PathVariable String userId);
}
