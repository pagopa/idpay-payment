package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
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
    PointOfSaleTransactionsListDTO getPointOfSaleTransactions(
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader(name = "x-point-of-sale-id", required = false) String tokenPointOfSaleId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("pointOfSaleId") String pointOfSaleId,
            @RequestParam(required = false) String fiscalCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productGtin,
            @RequestParam(required = false) String trxCode,
            @PageableDefault(sort = "trxChargeDate", direction = Sort.Direction.DESC) Pageable pageable
    );


    @GetMapping("/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions/processed")
    @ResponseStatus(code = HttpStatus.OK)
    PointOfSaleTransactionsListDTO getPointOfSaleTransactionsProcessed(
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader(name = "x-point-of-sale-id", required = false) String tokenPointOfSaleId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("pointOfSaleId") String pointOfSaleId,
            @RequestParam(required = false) String productGtin,
            @RequestParam(required = false) String fiscalCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trxCode,
            @PageableDefault(sort = "trxChargeDate", direction = Sort.Direction.DESC) Pageable pageable
    );


    @GetMapping("/initiatives/{initiativeId}/point-of-sales/{pointOfSaleId}/transactions/{transactionId}/download")
    @ResponseStatus(code = HttpStatus.OK)
    DownloadInvoiceResponseDTO downloadInvoiceFile(
            // x-merchant-id and x-point-of-sale-id are optional because the request can be signed with a JWT token from enti portal
            @RequestHeader(name = "x-merchant-id", required = false) String merchantId,
            @RequestHeader(name = "x-point-of-sale-id", required = false) String tokenPointOfSaleId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("pointOfSaleId") String pointOfSaleId,
            @PathVariable("transactionId") String transactionId
    );

}

