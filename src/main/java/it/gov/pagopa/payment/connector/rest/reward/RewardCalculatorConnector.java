package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface RewardCalculatorConnector {

  AuthPaymentDTO previewTransaction(TransactionInProgress transaction);
  AuthPaymentDTO authorizePayment(TransactionInProgress transaction);
  AuthPaymentDTO cancelTransaction(TransactionInProgress transaction);
}
