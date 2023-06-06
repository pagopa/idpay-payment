package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.MerchantTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

        when(repositoryMock.findByFilter(Mockito.any(), Mockito.any())).thenReturn(List.of(transaction1));

        MerchantTransactionDTO merchantTransaction = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        merchantTransaction.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction.setTrxDate(transaction1.getTrxDate().toLocalDateTime());

        MerchantTransactionsListDTO merchantTransactionsListDTO_expected = MerchantTransactionsListDTO.builder()
                .content(List.of(merchantTransaction))
                .pageSize(10).totalElements(1).totalPages(1).build();

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        Mockito.when(encryptRestConnector.upsertToken(Mockito.any())).thenReturn(encryptedCfDTO);
        Mockito.when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(1, result.getContent().size());
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

}
