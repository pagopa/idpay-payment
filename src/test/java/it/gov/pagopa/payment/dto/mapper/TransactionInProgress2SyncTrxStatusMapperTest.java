package it.gov.pagopa.payment.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransactionInProgress2SyncTrxStatusMapperTest {

    private TransactionInProgress2SyncTrxStatusMapper transactionInProgress2SyncTrxStatusMapper;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock = new TransactionInProgress2TransactionResponseMapper(5, "qrcodeImgBaseUrl", "qrcodeImgBaseUrl");


    @BeforeEach
    void setUp() {
        transactionInProgress2SyncTrxStatusMapper = new TransactionInProgress2SyncTrxStatusMapper(transactionInProgress2TransactionResponseMapperMock);
    }
    @Test
    void transactionInProgressMapper() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate", "qrcodePngUrl", "qrcodeTxtUrl");
    }

    @Test
    void transactionInProgressMapper_StatusCreated() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setStatus(SyncTrxStatus.CREATED);
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate");
    }

    @Test
    void transactionInProgressMapperQRCode_StatusCreated() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction.setStatus(SyncTrxStatus.CREATED);
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate");
    }

    @Test
    void transactionInProgressMapperQRCode_StatusAuthorized() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate");
    }

    @Test
    void transactionInProgressMapperBarCode_StatusAuthorized() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .reward(0L)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setChannel(RewardConstants.TRX_CHANNEL_BARCODE);
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);
        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);
        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate", "qrcodePngUrl", "qrcodeTxtUrl");
        Assertions.assertNull(result.getQrcodePngUrl());
        Assertions.assertNull(result.getQrcodeTxtUrl());
    }

    @Test
    void transactionInProgressMapper_withSplitPayment() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setReward(transaction.getAmountCents());

        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);

        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate", "qrcodePngUrl", "qrcodeTxtUrl");
    }

    @Test
    void transactionInProgressMapper_withoutSplitPayment() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstanceBuilder(1,SyncTrxStatus.REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        transaction.setReward(null);

        SyncTrxStatusDTO result= transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(transaction);

        mapperAssertion(transaction, result);
        TestUtils.checkNotNullFields(result, "trxChargeDate", "authDate", "qrcodePngUrl", "qrcodeTxtUrl", "rewardCents", "splitPayment", "residualAmountCents");
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
        });
    }
}