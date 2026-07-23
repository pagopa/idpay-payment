package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/idpay/transactions")
public interface TransactionsController {

    @GetMapping
    List<Transaction> findAll(
            @RequestParam(value = "idTrxIssuer", required = false) String idTrxIssuer,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "trxDateStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime trxDateStart,
            @RequestParam(value = "trxDateEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime trxDateEnd,
            @RequestParam(value = "amountCents", required = false) Long amountCents,
            @PageableDefault(size = 2000) Pageable pageable
    );


    @GetMapping("/{initiativeId}/{userId}")
    List<Transaction> findByInitiativeIdAndUserId(
            @PathVariable(value = "initiativeId") String initiativeId,
            @PathVariable(value = "userId") String userId
    );

}
