package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay")
public interface PointOfSaleTransactionController {

    @GetMapping("/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions")
    @ResponseStatus(code = HttpStatus.OK)
    PointOfSaleTransactionsListDTO getPointOfSaleTransactions(@RequestHeader("x-merchant-id") String merchantId,
                                                              @PathVariable("initiativeId") String initiativeId,
                                                              @PathVariable("pointOfSaleId") String pointOfSaleId,
                                                              @RequestParam(required = false) String fiscalCode,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String productGtin,
                                                              @PageableDefault(sort = "updateDate", direction = Sort.Direction.DESC) Pageable pageable);
}
