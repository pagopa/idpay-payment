package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/idpay/payment")
public interface CommonPaymentController {
    @PostMapping("/trx")
    @ResponseStatus(code = HttpStatus.CREATED)
    BaseTransactionResponseDTO createTransaction(
            @RequestBody @Valid TransactionCreationRequest trxCreationRequest,
            @RequestHeader("x-merchant-id") String merchantId,
            @RequestHeader("x-acquirer-id") String acquirerId,
            @RequestHeader("x-apim-request-id") String idTrxIssuer);
}
