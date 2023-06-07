package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/merchant/portal")
public interface MerchantTransactionController {
    @GetMapping("/initiatives/{initiativeId}/transactions")
    @ResponseStatus(code = HttpStatus.OK)
    MerchantTransactionsListDTO getMerchantTransactions(@RequestHeader("x-merchant-id") String merchantId,
                                                        @PathVariable("initiativeId") String initiativeId,
                                                        @RequestParam(required = false) String fiscalCode,
                                                        @RequestParam(required = false) String status,
                                                        @PageableDefault(sort="updateDate", direction = Sort.Direction.DESC) Pageable pageable);
}
