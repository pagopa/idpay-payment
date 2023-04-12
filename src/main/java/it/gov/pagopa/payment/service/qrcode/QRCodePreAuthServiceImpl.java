package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {

  @Override
  public TransactionResponse relateUser(String userId, String trxCode) {
    // Cerco trx -> 404
    // Associo utente -> 403 (stesso utente stato IDENTIFIED ok)
    // Anteprima premio -> 403 se utente non onboardato con stato REJECTED e causale NO_ACTIVE_INITIATIVES
    return null;
  }
}
