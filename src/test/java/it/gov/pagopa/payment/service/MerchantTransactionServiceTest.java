package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.MerchantTransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantTransactionServiceTest {

    private final String QRCODE_IMGURL = "QRCODE_IMGURL";
    private final String QRCODE_TXTURL = "QRCODE_TXTURL";

    @Mock private EncryptRestConnector encryptRestConnector;
    @Mock private DecryptRestConnector decryptRestConnector;
    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock;
    @Mock private TransactionService transactionService;

    private MerchantTransactionService service;

    @BeforeEach
    void setUp() {
        service = new MerchantTransactionServiceImpl(
                4320,
                decryptRestConnector,
                encryptRestConnector,
                transactionService,
                repositoryMock,
                transactionInProgress2TransactionResponseMapperMock
        );
    }


    @Test
    void getMerchantTransactionList() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(repositoryMock.findByFilter(any(), any())).thenReturn(List.of(transaction1, transaction2));

        MerchantTransactionDTO merchantTransaction1 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        merchantTransaction1.setUpdateDate(transaction1.getUpdateDate());
        merchantTransaction1.setTrxDate(transaction1.getTrxDate().toLocalDateTime());
        merchantTransaction1.setSplitPayment(true);
        merchantTransaction1.setResidualAmountCents(transaction1.getAmountCents() - transaction1.getRewardCents());

        MerchantTransactionDTO merchantTransaction2 = MerchantTransactionDTOFaker.mockInstance(1, SyncTrxStatus.CREATED);
        merchantTransaction2.setUpdateDate(transaction2.getUpdateDate());
        merchantTransaction2.setTrxDate(transaction2.getTrxDate().toLocalDateTime());
        merchantTransaction2.setFiscalCode(null);

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        when(encryptRestConnector.upsertToken(any())).thenReturn(encryptedCfDTO);
        when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction1.setUserId("USERID1");
        TransactionInProgress transaction2 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction2.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);

        when(repositoryMock.findByFilter(any(), any())).thenReturn(List.of(transaction1, transaction2));
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(anyString())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(anyString())).thenReturn(QRCODE_TXTURL);

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("MERCHANTFISCALCODE1");
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("USERID1");

        when(encryptRestConnector.upsertToken(any())).thenReturn(encryptedCfDTO);
        when(decryptRestConnector.getPiiByToken("USERID1")).thenReturn(decryptCfDTO);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(2, result.getContent().size());
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_ko_encrypt() {
        when(encryptRestConnector.upsertToken(any())).thenThrow(new RuntimeException("Encryption Error"));

        PDVInvocationException e = assertThrows(PDVInvocationException.class, () ->
                service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null)
        );
        assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
        assertEquals("An error occurred during encryption", e.getMessage());
    }

    @Test
    void getMerchantTransactionList_ko_decrypt() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
        transaction1.setUserId("USERID1");

        when(repositoryMock.findByFilter(any(), any())).thenReturn(List.of(transaction1));
        when(encryptRestConnector.upsertToken(any())).thenReturn(new EncryptedCfDTO("USERID1"));
        when(decryptRestConnector.getPiiByToken("USERID1")).thenThrow(new RuntimeException("Decryption Error"));

        PDVInvocationException e = assertThrows(PDVInvocationException.class, () ->
                service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null)
        );
        assertEquals("PAYMENT_GENERIC_ERROR", e.getCode());
        assertEquals("An error occurred during decryption", e.getMessage());
    }

    @Test
    void getMerchantTransactionList_NoFiscalCode_QRCODE() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction1.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        when(repositoryMock.findByFilter(any(), any())).thenReturn(List.of(transaction1));

        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeImgUrl(anyString())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapperMock.generateTrxCodeTxtUrl(anyString())).thenReturn(QRCODE_TXTURL);

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_NoFiscalCode() {
        TransactionInProgress transaction1 = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(repositoryMock.findByFilter(any(), any())).thenReturn(List.of(transaction1));

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", null, null, null);

        assertEquals(1, result.getContent().size());
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void getMerchantTransactionList_EmptyTransactionInProgressList() {
        when(repositoryMock.findByFilter(any(), any())).thenReturn(Collections.emptyList());
        when(encryptRestConnector.upsertToken(any())).thenReturn(new EncryptedCfDTO("USERID1"));

        MerchantTransactionsListDTO result = service.getMerchantTransactions("MERCHANTID1", "INITIATIVEID1", "MERCHANTFISCALCODE1", null, null);

        assertEquals(0, result.getContent().size());
        TestUtils.checkNotNullFields(result);
    }


    @Test
    void getMerchantTransactionsProcessed_success() {
        String merchantId = "MERCHANT_1";
        String initiativeId = "INITIATIVE_1";
        String fiscalCode = "FISCAL_CODE";
        String encryptedUserId = "ENCRYPTED_USER";
        Pageable pageable = PageRequest.of(0, 10);

        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setId("TX_ID_1");
        transaction.setAmountCents(1000L);
        transaction.setTrxDate(OffsetDateTime.now());
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);
        transaction.setRewardBatchStatusTrx(RewardBatchTrxStatus.TO_CHECK.name());

        Map<String, Reward> rewards = new HashMap<>();
        Reward reward = new Reward();
        reward.setAccruedRewardCents(150L);
        rewards.put(initiativeId, reward);
        transaction.setRewards(rewards);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        when(encryptRestConnector.upsertToken(any())).thenReturn(new EncryptedCfDTO(encryptedUserId));
        when(transactionService.getMerchantTransactionByFilter(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(transactionPage);

        MerchantTransactionsListDTO result = service.getMerchantTransactionsProcessed(
                merchantId, "ADMIN", initiativeId, fiscalCode, "AUTHORIZED",
                "BATCH_1", "TO_CHECK", "POS_1", "TRX_CODE_1", pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        MerchantTransactionDTO dto = result.getContent().getFirst();
        assertEquals("TX_ID_1", dto.getTrxId());
        assertEquals(RewardBatchTrxStatus.CONSULTABLE, dto.getRewardBatchTrxStatus());
        assertEquals(150L, dto.getRewardAmountCents());
        assertEquals(850L, dto.getAuthorizedAmountCents());
    }

    @Test
    void getMerchantTransactionsProcessed_withNullFieldsAndMissingRewards() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setPointOfSaleId(null);
        transaction.setFranchiseName(null);
        transaction.setRewards(null);
        transaction.setTrxDate(null);
        transaction.setRewardBatchStatusTrx(null);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        when(transactionService.getMerchantTransactionByFilter(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(transactionPage);

        MerchantTransactionsListDTO result = service.getMerchantTransactionsProcessed(
                "M1", "ADMIN", "I1", null, null,
                null, null, null, null, pageable
        );

        assertNotNull(result);
        MerchantTransactionDTO dto = result.getContent().getFirst();
        assertEquals("-", dto.getPointOfSaleId());
        assertEquals("-", dto.getFranchiseName());
        assertEquals(0L, dto.getRewardAmountCents());
        assertEquals(LocalDateTime.MIN, dto.getTrxDate());
        assertNull(dto.getRewardBatchTrxStatus());
    }

    @Test
    void getMerchantTransactionsProcessed_withExcludedOperator() {
        String merchantId = "MERCHANT_1";
        String initiativeId = "INITIATIVE_1";
        Pageable pageable = PageRequest.of(0, 10);

        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setRewardBatchStatusTrx(RewardBatchTrxStatus.TO_CHECK.name());

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        when(transactionService.getMerchantTransactionByFilter(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(transactionPage);

        MerchantTransactionsListDTO result = service.getMerchantTransactionsProcessed(
                merchantId, "operator1", initiativeId, null, null,
                null, null, null, null, pageable
        );

        assertNotNull(result);
        MerchantTransactionDTO dto = result.getContent().getFirst();
        assertEquals(RewardBatchTrxStatus.TO_CHECK, dto.getRewardBatchTrxStatus());
    }

    @Test
    void getMerchantTransactionsProcessed_invalidRewardStatus_throwsException() {
        PageRequest pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.getMerchantTransactionsProcessed(
                        "M1", "ADMIN", "I1", null, null,
                        null, "INVALID_STATUS", null, null, pageable
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid rewardBatchTrxStatus value"));
    }

    @Test
    void getMerchantTransactionsProcessed_invalidTrxStatus_throwsException() {
        PageRequest pageable = PageRequest.of(0, 10);

        TransactionMissingParametersException exception = assertThrows(TransactionMissingParametersException.class, () ->
                service.getMerchantTransactionsProcessed(
                        "M1", "ADMIN", "I1", null, "INVALID_TRX_STATUS",
                        null, null, null, null, pageable
                )
        );

        assertEquals("STATUS_NOT_ALLOWED", exception.getCode());
    }

    @Test
    void getProcessedTransactionStatuses_allStatuses_whenRoleHasAccess() {
        List<String> statuses = service.getProcessedTransactionStatuses("ADMIN");

        assertNotNull(statuses);
        assertTrue(statuses.contains("TO_CHECK"));
        assertEquals(RewardBatchTrxStatus.values().length, statuses.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator1", "operator2", "operator3", "OPERATOR1"})
    void getProcessedTransactionStatuses_excludeToCheck_whenOperatorExcluded(String role) {
        List<String> statuses = service.getProcessedTransactionStatuses(role);

        assertNotNull(statuses);
        assertFalse(statuses.contains("TO_CHECK"));
        assertEquals(RewardBatchTrxStatus.values().length - 1, statuses.size());
    }
}