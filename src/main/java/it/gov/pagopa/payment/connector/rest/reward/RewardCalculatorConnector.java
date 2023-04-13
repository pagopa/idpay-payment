package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface RewardCalculatorConnector {

  AuthPaymentDTO authorizePayment(TransactionInProgress transaction, AuthPaymentRequestDTO body);

  AuthPaymentDTO previewTransaction(TransactionInProgress transaction, AuthPaymentRequestDTO body);
}
