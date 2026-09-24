package it.gov.pagopa.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reward_transactions", schema = "idpay-rimborsi")
public class RewardTransaction {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "reward_batch_id")
    private String rewardBatchId;

    @Column(name = "reward_batch_trx_status")
    private String rewardBatchStatusTrx;
}

