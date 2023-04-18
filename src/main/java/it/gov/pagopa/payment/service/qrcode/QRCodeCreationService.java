package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodeCreationService {

  TransactionResponse createTransaction(
      TransactionCreationRequest trxCreationRequest,
      String channel,
      String merchantId,
      String acquirerId,
      String idTrxAcquirer);
}
