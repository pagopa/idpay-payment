package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTOFaker {
    public static TransactionDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionDTO.TransactionDTOBuilder mockInstanceBuilder(Integer bias) {
        return TransactionDTO.builder()
                .trxCode("TRXCODE%d".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .acquirerCode("ACQUIRERCODE%d".formatted(bias))
                .trxDate(LocalDateTime.now())
                .trxChargeDate(LocalDateTime.now())
                .authDate(LocalDateTime.now())
                .elaborationDateTime(LocalDateTime.now())
                .hpan("HPAN%s".formatted(bias))
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .operationTypeTranscoded(OperationType.CHARGE)
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .correlationId("CORRELATIONID%s".formatted(bias))
                .amountCents(10L)
                .effectiveAmount(BigDecimal.TEN)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .merchantId("MERCHANTID%s".formatted(bias))
                .senderCode("SENDERCODE%d".formatted(bias))
                .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
                .vat("VAT%d".formatted(bias))
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .userId("USERID%d".formatted(bias))
                .status(SyncTrxStatus.CREATED)
                .callbackUrl("CALLBACKURL%d".formatted(bias));
    }
}
