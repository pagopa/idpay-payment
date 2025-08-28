package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/idpay/payment/bar-code")
public interface BarCodePaymentController {

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    TransactionBarCodeResponse createTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );

    @PutMapping("/{trxCode}/authorize")
    @ResponseStatus(code = HttpStatus.OK)
    AuthPaymentDTO authPayment(
            @PathVariable("trxCode") String trxCode,
            @RequestBody @Valid AuthBarCodePaymentDTO authBarCodePaymentDTO,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId
    );

    @PostMapping("/preview")
    @ResponseStatus(code = HttpStatus.OK)
    PreviewPaymentDTO previewPayment(
            @RequestBody @Valid PreviewPaymentRequestDTO previewPaymentRequestDTO
    );

}
