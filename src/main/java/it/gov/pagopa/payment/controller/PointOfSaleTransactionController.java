package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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


    @GetMapping("/{pointOfSaleId}/transactions/{transactionId}/download")
    @ResponseStatus(code = HttpStatus.OK)
    DownloadInvoiceResponseDTO downloadInvoiceFile(
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader(name = "x-point-of-sale-id", required = false) String tokenPointOfSaleId,
            @PathVariable("pointOfSaleId") String pointOfSaleId,
            @PathVariable("transactionId") String transactionId
    );

    @PostMapping("/transactions/{transactionId}/reversal-invoiced")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void reversalTransactionInvoiced(
            @PathVariable("transactionId") String transactionId,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-point-of-sale-id") String pointOfSaleId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "docNumber", required = false) String docNumber
    );

    @PutMapping(path = "/transactions/{transactionId}/invoice/update")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void updateInvoiceFile(
            @PathVariable("transactionId") String transactionId,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-point-of-sale-id") String pointOfSaleId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "docNumber", required = false) String docNumber
    );

}

