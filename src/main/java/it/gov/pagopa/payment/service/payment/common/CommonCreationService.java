package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface CommonCreationService {
    TransactionResponse createTransaction(
            TransactionCreationRequest trxCreationRequest,
            String channel,
            String merchantId,
            String acquirerId,
            String idTrxIssuer);

}
