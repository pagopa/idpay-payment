package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantTransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private DecryptRestConnector decryptRestConnectorMock;
    @Mock
    private EncryptRestConnector encryptRestConnectorMock;
    @Mock
    private TransactionService transactionServiceMock;
    @Mock
    private TransactionMapper transactionMapperMock;

    private MerchantTransactionServiceImpl merchantTransactionService;

    private static final int EXPIRATION_MINUTES = 15;
    private static final String MERCHANT_ID = "MERCHANT_ID_1";
    private static final String INITIATIVE_ID = "INITIATIVE_ID_1";
    private static final String FISCAL_CODE = "ABCDEF90A01H501W";
    private static final String USER_ID_ENCRYPTED = "ENCRYPTED_USER_ID_1";
    private static final String TRX_CODE = "TRX_CODE_1";

    @BeforeEach
    void setUp() {
        merchantTransactionService = new MerchantTransactionServiceImpl(
                EXPIRATION_MINUTES,
                transactionRepositoryMock,
                decryptRestConnectorMock,
                encryptRestConnectorMock,
                transactionServiceMock,
                transactionMapperMock
        );
    }

    // ------------------------------------------------------------------------------------------------
    // getMerchantTransactions
    // ------------------------------------------------------------------------------------------------

    @Test
    void testGetMerchantTransactions_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction trx = createDummyTransaction();

        EncryptedCfDTO encryptedModelDTO = new EncryptedCfDTO();
        encryptedModelDTO.setToken(USER_ID_ENCRYPTED);

        DecryptCfDTO decryptedModelDTO = new DecryptCfDTO();
        decryptedModelDTO.setPii(FISCAL_CODE);

        when(encryptRestConnectorMock.upsertToken(any(CFDTO.class))).thenReturn(encryptedModelDTO);
        when(decryptRestConnectorMock.getPiiByToken(USER_ID_ENCRYPTED)).thenReturn(decryptedModelDTO);
        when(transactionRepositoryMock.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(trx)));
        when(transactionMapperMock.generateTrxCodeImgUrl(TRX_CODE)).thenReturn("http://img.url");
        when(transactionMapperMock.generateTrxCodeTxtUrl(TRX_CODE)).thenReturn("http://txt.url");

        MerchantTransactionsListDTO result = merchantTransactionService.getMerchantTransactions(
                MERCHANT_ID, INITIATIVE_ID, FISCAL_CODE, SyncTrxStatus.AUTHORIZED.name(), pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        MerchantTransactionDTO dto = result.getContent().getFirst();
        assertEquals(FISCAL_CODE, dto.getFiscalCode());

        verify(encryptRestConnectorMock, times(1)).upsertToken(any(CFDTO.class));
    }

    @Test
    void testGetMerchantTransactions_EncryptionError_ThrowsPDVInvocationException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(encryptRestConnectorMock.upsertToken(any(CFDTO.class)))
                .thenThrow(new RuntimeException("PDV Error"));

        assertThrows(PDVInvocationException.class, () ->
                merchantTransactionService.getMerchantTransactions(
                        MERCHANT_ID, INITIATIVE_ID, FISCAL_CODE, SyncTrxStatus.AUTHORIZED.name(), pageable
                )
        );
    }

    // ------------------------------------------------------------------------------------------------
    // getMerchantTransactionsProcessed
    // ------------------------------------------------------------------------------------------------

    @Test
    void testGetMerchantTransactionsProcessed_SuccessWithDefaultSortAndStatusExposure() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction trx = createDummyTransaction();
        trx.setRewardBatchStatusTrx(RewardBatchTrxStatus.TO_CHECK.name());
        Reward reward = new Reward();
        reward.setAccruedRewardCents(100L);
        trx.setRewards(Map.of(INITIATIVE_ID, reward));

        EncryptedCfDTO encryptedModelDTO = new EncryptedCfDTO();
        encryptedModelDTO.setToken(USER_ID_ENCRYPTED);
        when(encryptRestConnectorMock.upsertToken(any(CFDTO.class))).thenReturn(encryptedModelDTO);

        when(transactionServiceMock.getMerchantTransactionByFilter(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trx)));

        MerchantTransactionsListDTO result = merchantTransactionService.getMerchantTransactionsProcessed(
                MERCHANT_ID, "adminRole", INITIATIVE_ID, FISCAL_CODE, "REWARDED",
                "BATCH_1", "CONSULTABLE", "POS_1", TRX_CODE, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        MerchantTransactionDTO dto = result.getContent().getFirst();

        // TO_CHECK esposto come CONSULTABLE per ruoli autorizzati
        assertEquals(RewardBatchTrxStatus.CONSULTABLE, dto.getRewardBatchTrxStatus());
        assertEquals(100L, dto.getRewardAmountCents());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionServiceMock).getMerchantTransactionByFilter(any(TrxFiltersDTO.class), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isSorted());
        assertEquals("rewardBatchStatusTrx", capturedPageable.getSort().iterator().next().getProperty());
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().iterator().next().getDirection());
    }

    @Test
    void testGetMerchantTransactionsProcessed_InvalidStatus_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(TransactionMissingParametersException.class, () ->
                merchantTransactionService.getMerchantTransactionsProcessed(
                        MERCHANT_ID, "adminRole", INITIATIVE_ID, null, "INVALID_STATUS",
                        null, null, null, null, pageable
                )
        );
    }

    @Test
    void testGetMerchantTransactionsProcessed_InvalidRewardBatchTrxStatus_ThrowsResponseStatusException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(ResponseStatusException.class, () ->
                merchantTransactionService.getMerchantTransactionsProcessed(
                        MERCHANT_ID, "adminRole", INITIATIVE_ID, null, "REWARDED",
                        null, "INVALID_ENUM", null, null, pageable
                )
        );
    }

    // ------------------------------------------------------------------------------------------------
    // getProcessedTransactionStatuses
    // ------------------------------------------------------------------------------------------------

    @Test
    void testGetProcessedTransactionStatuses_FullAccess() {
        List<String> statuses = merchantTransactionService.getProcessedTransactionStatuses("adminRole");

        assertNotNull(statuses);
        assertTrue(statuses.contains("TO_CHECK"));
        assertEquals(RewardBatchTrxStatus.values().length, statuses.size());
    }

    @Test
    void testGetProcessedTransactionStatuses_ExcludedOperator_FiltersToCheck() {
        List<String> statuses = merchantTransactionService.getProcessedTransactionStatuses("operator1");

        assertNotNull(statuses);
        assertFalse(statuses.contains("TO_CHECK"));
        assertEquals(RewardBatchTrxStatus.values().length - 1, statuses.size());
    }

    private Transaction createDummyTransaction() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setId("TRX_ID_1");
        trx.setTrxCode(TRX_CODE);
        trx.setUserId(USER_ID_ENCRYPTED);
        trx.setAmountCents(1000L);
        trx.setRewardCents(200L);
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setStatus(SyncTrxStatus.AUTHORIZED);
        trx.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        return trx;
    }
}