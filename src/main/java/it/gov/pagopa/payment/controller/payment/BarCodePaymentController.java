package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment")
public interface BarCodePaymentController {

    @PostMapping("/bar-code")
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionBarCodeResponse createTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );

    @PutMapping("/bar-code/{trxCode}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(
            @PathVariable("trxCode") String trxCode,
            @RequestBody @Valid AuthBarCodePaymentDTO authBarCodePaymentDTO,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-point-of-sale-id") String pointOfSaleId,
            @RequestHeader("x-acquirer-id") String acquirerId
    );

    @PutMapping("/bar-code/{trxCode}/preview")
    @ResponseStatus(code = HttpStatus.OK)
    PreviewPaymentDTO previewPayment(
            @PathVariable("trxCode") String trxCode,
            @RequestBody @Valid PreviewPaymentRequestDTO previewPaymentRequestDTO
    );

    @GetMapping("/initiatives/{initiativeId}/bar-code")
    @ResponseStatus(code = HttpStatus.OK)
    TransactionBarCodeResponse retrievePayment(
            @PathVariable("initiativeId") String initiativeId,
            @RequestHeader("x-user-id") String userId
    );

    @PutMapping("/bar-code/{trxCode}/capture")
    TransactionBarCodeResponse capturePayment(
            @PathVariable("trxCode") String trxCode);

    @PostMapping("/bar-code/extended")
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionBarCodeEnrichedResponse createExtendedTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );

}
