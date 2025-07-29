package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.PointOfSaleTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointOfSaleTransactionServiceTest {

    private final String QRCODE_IMGURL = "QRCODE_IMGURL";
    private final String QRCODE_TXTURL = "QRCODE_TXTURL";
    @Mock
    private EncryptRestConnector encryptRestConnector;
    @Mock
    private DecryptRestConnector decryptRestConnector;
    @Mock
    private TransactionInProgressRepository repositoryMock;
    @Mock
    private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock;

    private PointOfSaleTransactionService pointOfSaleTransactionService;

    @BeforeEach
    void setUp() {
        pointOfSaleTransactionService = new PointOfSaleTransactionServiceImpl(4320, decryptRestConnector, encryptRestConnector, repositoryMock, transactionInProgress2TransactionResponseMapperMock);
    }

    @Test
    void getPointOfSaleTransactionList() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1, transaction2));

        PointOfSaleTransactionDTO expectedDto1 = buildExpectedDTO(transaction1, true, false);
        PointOfSaleTransactionDTO expectedDto2 = buildExpectedDTO(transaction2, false, false);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(expectedDto1, expectedDto2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        mockEncryptDecrypt();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction1.setUserId("USERID1");

        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction2.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1, transaction2));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(Mockito.anyString())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(Mockito.anyString())).thenReturn(QRCODE_TXTURL);

        PointOfSaleTransactionDTO expectedDto1 = buildExpectedDTO(transaction1, true, true);
        PointOfSaleTransactionDTO expectedDto2 = buildExpectedDTO(transaction2, false, true);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(expectedDto1, expectedDto2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        mockEncryptDecrypt();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_ko_encrypt() {
        try {
            pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (PDVInvocationException e) {
            assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
            assertEquals("An error occurred during encryption", e.getMessage());
        }
    }

    @Test
    void getPointOfSaleTransactionList_ko_decrypt() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
        transaction1.setUserId("USERID1");

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);

        try {
            pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (PDVInvocationException e) {
            assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
            assertEquals("An error occurred during decryption", e.getMessage());
        }
    }

    @Test
    void getPointOfSaleTransactionList_NoFiscalCode_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(Mockito.anyString())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(Mockito.anyString())).thenReturn(QRCODE_TXTURL);

        PointOfSaleTransactionDTO pointOfSaleTransaction = buildExpectedDTO(transaction1, false, true);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_NoFiscalCode() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        PointOfSaleTransactionDTO pointOfSaleTransaction = buildExpectedDTO(transaction1, false, false);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_EmptyTransactionInProgressList() {

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(Collections.emptyList())
                .pageSize(10).totalElements(0).totalPages(0).build();

        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(0, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_NullChannel() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction1.setChannel(null);
        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(Mockito.anyString())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(Mockito.anyString())).thenReturn(QRCODE_TXTURL);

        PointOfSaleTransactionDTO expectedDto = buildExpectedDTO(transaction1, false, true);

        PointOfSaleTransactionsListDTO expectedResult = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(expectedDto))
                .pageSize(10).totalElements(1).totalPages(1).build();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(expectedResult, result);
        TestUtils.checkNotNullFields(result);
    }

    private void mockEncryptDecrypt() {
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");
        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);
    }

    private PointOfSaleTransactionDTO buildExpectedDTO(TransactionInProgress trx, boolean includeFiscalCode, boolean isQRCode) {
        PointOfSaleTransactionDTO.PointOfSaleTransactionDTOBuilder builder =
                PointOfSaleTransactionDTOFaker.mockInstanceBuilder(1, trx.getStatus());

        builder.updateDate(trx.getUpdateDate());
        builder.trxDate(trx.getTrxDate().toLocalDateTime());
        builder.channel(trx.getChannel());

        if (trx.getRewardCents() != null) {
            builder.splitPayment(true);
            builder.residualAmountCents(trx.getAmountCents() - trx.getRewardCents());
        }
        if (!includeFiscalCode) {
            builder.fiscalCode(null);
        }
        if (isQRCode) {
            builder.qrcodePngUrl(QRCODE_IMGURL);
            builder.qrcodeTxtUrl(QRCODE_TXTURL);
        }

        return builder.build();
    }
}
