package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionSynchronousException extends RuntimeException{
  private final transient AuthPaymentDTO response;

  public TransactionSynchronousException(AuthPaymentDTO response) {
    this.response = response;
  }
}
