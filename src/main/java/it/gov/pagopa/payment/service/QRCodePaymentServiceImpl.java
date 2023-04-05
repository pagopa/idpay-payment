package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import org.springframework.stereotype.Service;

@Service
public class QRCodePaymentServiceImpl implements
    QRCodePaymentService {

  @Override
  public TransactionCreated createTransaction(TransactionCreationRequest trxCreationRequest) {

    // Controllo esistenza iniziativa su reward_rule
    // Non esiste -> 404

    // TODO Genero trxCode

    // Ritorno nuova transaction_in_progress in stato CREATED

    return null;
  }
}
