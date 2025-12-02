package it.gov.pagopa.payment.connector.rest.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletDTO {

    private String familyId;
    private String initiativeId;
    private String initiativeName;
    private String status;
    private String iban;
    private LocalDate endDate;
    private int nInstr;
    private Long amountCents;
    private Long initialAmoutCents; // total budget assigned
    private Long accruedCents;
    private Long refundedCents;
    private LocalDateTime lastCounterUpdate;
    private String initiativeRewardType;
    private String logoURL;
    private String organizationName;
    private Long nTrx;
    private Long maxTrx;

}
