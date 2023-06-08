package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.MerchantTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class MerchantTransactionServiceTest {
    @Mock
    private EncryptRestConnector encryptRestConnector;
    @Mock
    private DecryptRestConnector decryptRestConnector;
    @Mock private TransactionInProgressRepository repositoryMock;

    MerchantTransactionService service;

    @BeforeEach
    void setUp() {
        service = new MerchantTransactionServiceImpl(decryptRestConnector, encryptRestConnector, repositoryMock);
    }

    @Test
    void getMerchantTransactionList() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1, transaction2));

        MerchantTransactionDTO merchantTransaction1 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        merchantTransaction1.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction1.setTrxDate(transaction1.getTrxDate().toLocalDateTime());

        MerchantTransactionDTO merchantTransaction2 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction2.setUpdateDate(transaction2.getUpdateDate());
        merchantTransaction2.setTrxDate(transaction2.getTrxDate().toLocalDateTime());
        merchantTransaction2.setFiscalCode(null);

        MerchantTransactionsListDTO merchantTransactionsListDTO_expected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction1, merchantTransaction2))
                .pageSize(10).totalElements(2).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        assertEquals(merchantTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_ko_encrypt() {
        try {
            service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);
        } catch (ClientExceptionWithBody e){
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
            assertEquals("INTERNAL SERVER ERROR", e.getCode());
            assertEquals("Error during encryption", e.getMessage());
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
        } catch (ClientExceptionWithBody e){
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
            assertEquals("INTERNAL SERVER ERROR", e.getCode());
            assertEquals("Error during decryption, userId: [USERID1]", e.getMessage());
        }
    }

    @Test
    void getMerchantTransactionList_NoFiscalCode() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        MerchantTransactionDTO merchantTransaction = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        merchantTransaction.setFiscalCode(null);

        MerchantTransactionsListDTO merchantTransactionsListDTO_expected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(merchantTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_EmptyTransactionInProgressList() {

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());

        MerchantTransactionsListDTO merchantTransactionsListDTO_expected = MerchantTransactionsListDTO.builder()
                .content(Collections.emptyList())
                .pageSize(10).totalElements(0).totalPages(0).build();

        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(0, result.getContent().size());
        assertEquals(merchantTransactionsListDTO_expected, result);
        TestUtils.checkNotNullFields(result);
    }

}
