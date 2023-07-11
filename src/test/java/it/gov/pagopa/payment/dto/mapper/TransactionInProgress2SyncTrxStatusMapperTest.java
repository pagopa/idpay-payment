package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionInProgress2SyncTrxStatusMapperTest {
    private TransactionInProgress2SyncTrxStatusMapper transactionInProgress2SyncTrxStatusMapper;
    @BeforeEach
    void setUp() {
        transactionInProgress2SyncTrxStatusMapper = new TransactionInProgress2SyncTrxStatusMapper();
    }

    @Test
    void transactionInProgressMapper() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
    }

    public static void mapperAssertion(TransactionInProgress transaction, SyncTrxStatusDTO result) {
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(transaction.getId(), result.getId());
            assertEquals(transaction.getIdTrxIssuer(), result.getIdTrxIssuer());
            assertEquals(transaction.getTrxCode(), result.getTrxCode());
            assertEquals(transaction.getTrxDate(), result.getTrxDate());
            assertEquals(transaction.getTrxChargeDate(), result.getAuthDate());
            assertEquals(transaction.getOperationTypeTranscoded(), result.getOperationType());
            assertEquals(transaction.getAmountCents(), result.getAmountCents());
            assertEquals(transaction.getAmountCurrency(), result.getAmountCurrency());
            assertEquals(transaction.getMcc(), result.getMcc());
            assertEquals(transaction.getAcquirerId(), result.getAcquirerId());
            assertEquals(transaction.getMerchantId(), result.getMerchantId());
            assertEquals(transaction.getInitiativeId(), result.getInitiativeId());
            assertEquals(transaction.getReward(), result.getRewardCents());
            assertEquals(transaction.getRejectionReasons(), result.getRejectionReasons());
            assertEquals(transaction.getStatus(), result.getStatus());
            TestUtils.checkNotNullFields(result);
        });
    }
}