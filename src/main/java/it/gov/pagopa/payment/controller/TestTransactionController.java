package it.gov.pagopa.payment.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;

@RequestMapping("/idpay/internal/test-support")
public interface TestTransactionController {

    @PatchMapping("/transactions/{id}/end-date")
    ResponseEntity<Void> updateTrxEndDate(
            @PathVariable("id") String id,
            @RequestParam("initiativeId") String initiativeId,
            @RequestParam("userId") String userId,
            @RequestParam("newEndDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime newEndDate);
}
