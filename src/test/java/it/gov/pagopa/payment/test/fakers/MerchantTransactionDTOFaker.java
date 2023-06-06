package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.MerchantTransactionDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MerchantTransactionDTOFaker {
    public static MerchantTransactionDTO mockInstance(Integer bias, SyncTrxStatus status) {
        return mockInstanceBuilder(bias, status).build();
    }

    @Value("${app.qrCode.trxInProgressLifetimeMinutes}")
    static int trxInProgressLifetimeMinutes;

    public static MerchantTransactionDTO.MerchantTransactionDTOBuilder mockInstanceBuilder(Integer bias,
                                                                                         SyncTrxStatus status) {

        String id = "MOCKEDTRANSACTION_qr-code_%d".formatted(bias);

        return MerchantTransactionDTO.builder()
                .correlationId(id)
                .fiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
                .effectiveAmount(BigDecimal.TEN.setScale(2, RoundingMode.UNNECESSARY))
                .trxCode("trxcode%d".formatted(bias))
                .trxDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .trxExpirationMinutes(trxInProgressLifetimeMinutes)
                .status(status.toString())
                .updateDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
