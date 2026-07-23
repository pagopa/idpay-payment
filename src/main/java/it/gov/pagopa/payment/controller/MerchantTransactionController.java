package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/idpay/merchant/portal")
public interface MerchantTransactionController {

    @GetMapping("/initiatives/{initiativeId}/transactions")
    @ResponseStatus(code = HttpStatus.OK)
    MerchantTransactionsListDTO getMerchantTransactions(@RequestHeader("x-merchant-id") String merchantId,
                                                        @PathVariable("initiativeId") String initiativeId,
                                                        @RequestParam(required = false) String fiscalCode,
                                                        @RequestParam(required = false) String status,
                                                        @PageableDefault(sort = "updateDate", direction = Sort.Direction.DESC) Pageable pageable);


    @GetMapping("/initiatives/{initiativeId}/transactions/processed")
    MerchantTransactionsListDTO getMerchantTransactionsProcessed(@RequestHeader("x-merchant-id") String merchantId,
                                                                 @RequestHeader(value = "x-organization-role", required = false) String organizationRole,
                                                                 @PathVariable("initiativeId") String initiativeId,
                                                                 @RequestParam(required = false) String fiscalCode,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String rewardBatchId,
                                                                 @RequestParam(required = false) String rewardBatchTrxStatus,
                                                                 @RequestParam(required = false) String pointOfSaleId,
                                                                 @RequestParam(required = false) String trxCode,
                                                                 @PageableDefault Pageable pageable);

    @GetMapping("/initiatives/transactions/processed/statuses")
    List<String> getProcessedTransactionStatuses(@RequestHeader(value = "x-organization-role", required = false) String organizationRole);
}
