package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class BaseTransactionResponse2TransactionResponseMapper
    implements Function<BaseTransactionResponseDTO, TransactionResponse> {

  private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
  private final int authorizationExpirationMinutes;

  public BaseTransactionResponse2TransactionResponseMapper(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper,
                                                           @Value("${app.qrCode.expirations.authorizationMinutes}") int authorizationExpirationMinutes)
                                                          {
    this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    this.authorizationExpirationMinutes = authorizationExpirationMinutes;

  }

  @Override
  public TransactionResponse apply(BaseTransactionResponseDTO baseTransactionResponse) {
    return TransactionResponse.builder()
            .acquirerId(baseTransactionResponse.getAcquirerId())
            .amountCents(baseTransactionResponse.getAmountCents())
            .amountCurrency(baseTransactionResponse.getAmountCurrency())
            .idTrxAcquirer(baseTransactionResponse.getIdTrxAcquirer())
            .idTrxIssuer(baseTransactionResponse.getIdTrxIssuer())
            .initiativeId(baseTransactionResponse.getInitiativeId())
            .mcc(baseTransactionResponse.getMcc())
            .id(baseTransactionResponse.getId())
            .merchantId(baseTransactionResponse.getMerchantId())
            .trxDate(baseTransactionResponse.getTrxDate())
            .trxCode(baseTransactionResponse.getTrxCode())
            .status(baseTransactionResponse.getStatus())
            .merchantFiscalCode(baseTransactionResponse.getMerchantFiscalCode())
            .vat(baseTransactionResponse.getVat())
            .splitPayment(baseTransactionResponse.getSplitPayment())
            .residualAmountCents(baseTransactionResponse.getResidualAmountCents())
            .trxExpirationMinutes(authorizationExpirationMinutes)
            .qrcodePngUrl(transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(baseTransactionResponse.getTrxCode()))
            .qrcodeTxtUrl(transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(baseTransactionResponse.getTrxCode()))
            .build();
  }

}
