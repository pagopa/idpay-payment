package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.ExpiredTransactionsProcessedDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/transactions/expired")
public interface ExpiredTransactionsController {

    @PostMapping("/initiatives/{initiativeId}/update-status")
    @ResponseStatus(code = HttpStatus.OK)
    public ExpiredTransactionsProcessedDTO findAndUpdateStatus(@PathVariable("initiativeId") String initiativeId);

    @PostMapping("/initiatives/{initiativeId}/resend")
    @ResponseStatus(code = HttpStatus.OK)
    public ExpiredTransactionsProcessedDTO findAndSendStaleExpired(@PathVariable("initiativeId") String initiativeId);

}
