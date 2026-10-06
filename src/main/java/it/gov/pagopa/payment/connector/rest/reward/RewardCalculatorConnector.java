package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;

public interface RewardCalculatorConnector {

  AuthPaymentDTO previewTransaction(Transaction transaction);
  AuthPaymentDTO authorizePayment(Transaction transaction);
  AuthPaymentDTO cancelTransaction(Transaction transaction);
}
