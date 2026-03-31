package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.MerchantTransactionDTOFaker;
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
class MerchantTransactionServiceTest {

    private final String qrcodeImgurl ="QRCODE_IMGURL";
    private final String qrcodeTxturl ="QRCODE_TXTURL";
    @Mock private EncryptRestConnector encryptRestConnector;
    @Mock private DecryptRestConnector decryptRestConnector;
    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock;

    private MerchantTransactionService service;

    @BeforeEach
    void setUp() {
        service = new MerchantTransactionServiceImpl(4320, decryptRestConnector, encryptRestConnector, repositoryMock, transactionInProgress2TransactionResponseMapperMock);
    }

    @Test
    void getMerchantTransactionList() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1, transaction2));

        MerchantTransactionDTO merchantTransaction1 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        merchantTransaction1.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction1.setTrxDate(transaction1.getTrxDate());
        merchantTransaction1.setSplitPayment(true);
        merchantTransaction1.setResidualAmountCents(transaction1.getAmountCents()-transaction1.getRewardCents());


        MerchantTransactionDTO merchantTransaction2 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction2.setUpdateDate(transaction2.getUpdateDate());
        merchantTransaction2.setTrxDate(transaction2.getTrxDate());
        merchantTransaction2.setFiscalCode(null);


        MerchantTransactionsListDTO merchantTransactionsListDTOExpected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction1, merchantTransaction2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(merchantTransactionsListDTOExpected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction2.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1, transaction2));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(Mockito.anyString())).thenReturn(qrcodeImgurl);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(Mockito.anyString())).thenReturn(qrcodeTxturl);

        MerchantTransactionDTO merchantTransaction1 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        merchantTransaction1.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction1.setTrxDate(transaction1.getTrxDate());
        merchantTransaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        merchantTransaction1.setQrcodePngUrl(qrcodeImgurl);
        merchantTransaction1.setQrcodeTxtUrl(qrcodeTxturl);
        merchantTransaction1.setSplitPayment(true);
        merchantTransaction1.setResidualAmountCents(transaction1.getAmountCents()-transaction1.getRewardCents());

        MerchantTransactionDTO merchantTransaction2 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction2.setUpdateDate(transaction2.getUpdateDate());
        merchantTransaction2.setTrxDate(transaction2.getTrxDate());
        merchantTransaction2.setFiscalCode(null);
        merchantTransaction2.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        merchantTransaction2.setQrcodePngUrl(qrcodeImgurl);
        merchantTransaction2.setQrcodeTxtUrl(qrcodeTxturl);

        MerchantTransactionsListDTO merchantTransactionsListDTOExpected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction1, merchantTransaction2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(merchantTransactionsListDTOExpected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_ko_encrypt() {
        try {
            service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (PDVInvocationException e){
            assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
            assertEquals("An error occurred during encryption", e.getMessage());
        }
    }

    @Test
    void getMerchantTransactionList_ko_decrypt() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
        transaction1.setUserId("USERID1");

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);

        try {
            service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (PDVInvocationException e){
            assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
            assertEquals("An error occurred during decryption", e.getMessage());
        }
    }

    @Test
    void getMerchantTransactionList_NoFiscalCode_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(Mockito.anyString())).thenReturn(qrcodeImgurl);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(Mockito.anyString())).thenReturn(qrcodeTxturl);

        MerchantTransactionDTO merchantTransaction = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction.setTrxDate(transaction1.getTrxDate());
        merchantTransaction.setFiscalCode(null);
        merchantTransaction.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        merchantTransaction.setQrcodePngUrl(qrcodeImgurl);
        merchantTransaction.setQrcodeTxtUrl(qrcodeTxturl);

        MerchantTransactionsListDTO merchantTransactionsListDTOExpected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(merchantTransactionsListDTOExpected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_NoFiscalCode() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));


        MerchantTransactionDTO merchantTransaction = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction.setTrxDate(transaction1.getTrxDate());
        merchantTransaction.setFiscalCode(null);


        MerchantTransactionsListDTO merchantTransactionsListDTOExpected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(merchantTransactionsListDTOExpected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_EmptyTransactionInProgressList() {

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());

        MerchantTransactionsListDTO merchantTransactionsListDTOExpected = MerchantTransactionsListDTO.builder()
                .content(Collections.emptyList())
                .pageSize(10).totalElements(0).totalPages(0).build();

        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(0, result.getContent().size());
        assertEquals(merchantTransactionsListDTOExpected, result);
        TestUtils.checkNotNullFields(result);
    }

}
