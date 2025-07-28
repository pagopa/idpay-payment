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

    private final String QRCODE_IMGURL ="QRCODE_IMGURL";
    private final String QRCODE_TXTURL ="QRCODE_TXTURL";
    @Mock private EncryptRestConnector encryptRestConnector;
    @Mock private DecryptRestConnector decryptRestConnector;
    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock;

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

        PointOfSaleTransactionDTO pointOfSaleTransaction1 = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        pointOfSaleTransaction1.setUpdateDate(transaction1.getUpdateDate());
        pointOfSaleTransaction1.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction1.setSplitPayment(true);
        pointOfSaleTransaction1.setResidualAmountCents(transaction1.getAmountCents()-transaction1.getRewardCents());


        PointOfSaleTransactionDTO pointOfSaleTransaction2 = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransaction2.setUpdateDate(transaction2.getUpdateDate());
        pointOfSaleTransaction2.setTrxDate(transaction2.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction2.setFiscalCode(null);


        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction1, pointOfSaleTransaction2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

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

        PointOfSaleTransactionDTO pointOfSaleTransaction1 = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        pointOfSaleTransaction1.setUpdateDate(transaction1.getUpdateDate());
        pointOfSaleTransaction1.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        pointOfSaleTransaction1.setQrcodePngUrl(QRCODE_IMGURL);
        pointOfSaleTransaction1.setQrcodeTxtUrl(QRCODE_TXTURL);
        pointOfSaleTransaction1.setSplitPayment(true);
        pointOfSaleTransaction1.setResidualAmountCents(transaction1.getAmountCents()-transaction1.getRewardCents());

        PointOfSaleTransactionDTO pointOfSaleTransaction2 = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransaction2.setUpdateDate(transaction2.getUpdateDate());
        pointOfSaleTransaction2.setTrxDate(transaction2.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction2.setFiscalCode(null);
        pointOfSaleTransaction2.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        pointOfSaleTransaction2.setQrcodePngUrl(QRCODE_IMGURL);
        pointOfSaleTransaction2.setQrcodeTxtUrl(QRCODE_TXTURL);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction1, pointOfSaleTransaction2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_ko_encrypt() {
        try {
            pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (PDVInvocationException e){
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
        } catch (PDVInvocationException e){
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

        PointOfSaleTransactionDTO pointOfSaleTransaction = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransaction.setUpdateDate(transaction1.getUpdateDate());
        pointOfSaleTransaction.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction.setFiscalCode(null);
        pointOfSaleTransaction.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        pointOfSaleTransaction.setQrcodePngUrl(QRCODE_IMGURL);
        pointOfSaleTransaction.setQrcodeTxtUrl(QRCODE_TXTURL);

        PointOfSaleTransactionsListDTO pointOfSaleTransactionsListDTO_expected = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1","POINTOFSALEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(pointOfSaleTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getPointOfSaleTransactionList_NoFiscalCode() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));


        PointOfSaleTransactionDTO pointOfSaleTransaction = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransaction.setUpdateDate(transaction1.getUpdateDate());
        pointOfSaleTransaction.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction.setFiscalCode(null);


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

        PointOfSaleTransactionDTO pointOfSaleTransaction = PointOfSaleTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        pointOfSaleTransaction.setUpdateDate(transaction1.getUpdateDate());
        pointOfSaleTransaction.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        pointOfSaleTransaction.setChannel(null);
        pointOfSaleTransaction.setQrcodePngUrl(QRCODE_IMGURL);
        pointOfSaleTransaction.setQrcodeTxtUrl(QRCODE_TXTURL);
        pointOfSaleTransaction.setFiscalCode(null);

        PointOfSaleTransactionsListDTO expectedResult = PointOfSaleTransactionsListDTO.builder()
                .content(List.of(pointOfSaleTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        PointOfSaleTransactionsListDTO result = pointOfSaleTransactionService.getPointOfSaleTransactions("MERCHANTID1", "INITIATIVEID1", "POINTOFSALEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(expectedResult, result);
        TestUtils.checkNotNullFields(result);
    }

}
