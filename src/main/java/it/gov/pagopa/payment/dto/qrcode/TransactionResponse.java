package it.gov.pagopa.payment.dto.qrcode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

  private String id;
  private String trxCode;
  private String initiativeId;
  private String senderCode;
  private String merchantId;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  private LocalDateTime trxDate;
  private BigDecimal amount;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;

}
