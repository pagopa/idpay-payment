package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodeCreationService {

  TransactionResponse createQRCodeTransaction(
      TransactionCreationRequest trxCreationRequest,
      String channel,
      String merchantId,
      String acquirerId,
      String idTrxIssuer);
}
