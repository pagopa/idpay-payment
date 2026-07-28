package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonCreationServiceImplTest {

    @Mock
    private RewardRuleRepository rewardRuleRepositoryMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private MerchantConnector merchantConnectorMock;
    @Mock
    private TransactionService transactionServiceMock;
    @Mock
    private TransactionMapper transactionMapperMock;
    @InjectMocks
    private CommonCreationServiceImpl commonCreationService;

    private static final String INITIATIVE_ID = "INITIATIVE_ID_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String ACQUIRER_ID = "ACQUIRER_ID_123";
    private static final String CHANNEL = "QR_CODE";
    private static final String ID_TRX_ISSUER = "ISSUER_123";
    private static final String TRX_ID = "TRX_ID_123";
    private static final String TRX_CODE = "TRX_CODE_123";

    @Test
    void testCreateTransaction_Success() {
        // Given
        TransactionCreationRequest request = createDummyRequest(1000L);
        InitiativeConfig initiativeConfig = createDummyInitiativeConfig(
                InitiativeRewardType.DISCOUNT,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );
        RewardRule rewardRule = new RewardRule();
        rewardRule.setInitiativeConfig(initiativeConfig);

        MerchantDetailDTO merchantDetail = new MerchantDetailDTO();
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setTrxCode(TRX_CODE);

        TransactionResponse expectedResponse = new TransactionResponse();

        when(rewardRuleRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.of(rewardRule));
        when(merchantConnectorMock.merchantDetail(MERCHANT_ID, INITIATIVE_ID)).thenReturn(merchantDetail);
        when(transactionMapperMock.transactionCreationRequestToTransaction(
                request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, merchantDetail, ID_TRX_ISSUER
        )).thenReturn(transaction);
        when(transactionMapperMock.transactionToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse result = commonCreationService.createTransaction(
                request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER
        );

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);

        verify(transactionServiceMock, times(1)).generateTrxCodeAndSave(transaction, "CREATE_TRANSACTION");
        verify(auditUtilitiesMock, times(1)).logCreatedTransaction(INITIATIVE_ID, TRX_ID, TRX_CODE, MERCHANT_ID);
        verify(auditUtilitiesMock, never()).logErrorCreatedTransaction(any(), any());
    }

    @Test
    void testCreateTransaction_InvalidAmount() {
        // Given
        TransactionCreationRequest request = createDummyRequest(0L);

        // When & Then
        TransactionInvalidException exception = assertThrows(
                TransactionInvalidException.class,
                () -> commonCreationService.createTransaction(request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER)
        );

        assertEquals(PaymentConstants.ExceptionCode.AMOUNT_NOT_VALID, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorCreatedTransaction(INITIATIVE_ID, MERCHANT_ID);
        verifyNoInteractions(merchantConnectorMock, transactionServiceMock);
    }

    @Test
    void testCreateTransaction_InitiativeNotFound() {
        // Given
        TransactionCreationRequest request = createDummyRequest(1000L);
        when(rewardRuleRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.empty());

        // When & Then
        InitiativeNotfoundException exception = assertThrows(
                InitiativeNotfoundException.class,
                () -> commonCreationService.createTransaction(request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER)
        );

        assertTrue(exception.getMessage().contains("Cannot find initiative with id"));
        verify(auditUtilitiesMock, times(1)).logErrorCreatedTransaction(INITIATIVE_ID, MERCHANT_ID);
    }

    @Test
    void testCreateTransaction_InitiativeNotDiscount() {
        // Given
        TransactionCreationRequest request = createDummyRequest(1000L);
        InitiativeConfig initiativeConfig = createDummyInitiativeConfig(
                InitiativeRewardType.REFUND,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );
        RewardRule rewardRule = new RewardRule();
        rewardRule.setInitiativeConfig(initiativeConfig);

        when(rewardRuleRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.of(rewardRule));

        // When & Then
        InitiativeNotfoundException exception = assertThrows(
                InitiativeNotfoundException.class,
                () -> commonCreationService.createTransaction(request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER)
        );

        assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorCreatedTransaction(INITIATIVE_ID, MERCHANT_ID);
    }

    @Test
    void testCreateTransaction_InitiativeExpired() {
        // Given
        TransactionCreationRequest request = createDummyRequest(1000L);
        InitiativeConfig initiativeConfig = createDummyInitiativeConfig(
                InitiativeRewardType.DISCOUNT,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1)
        );
        RewardRule rewardRule = new RewardRule();
        rewardRule.setInitiativeConfig(initiativeConfig);

        when(rewardRuleRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.of(rewardRule));

        // When & Then
        InitiativeInvalidException exception = assertThrows(
                InitiativeInvalidException.class,
                () -> commonCreationService.createTransaction(request, CHANNEL, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER)
        );

        assertTrue(exception.getMessage().contains("Cannot create transaction out of valid period"));
        verify(auditUtilitiesMock, times(1)).logErrorCreatedTransaction(INITIATIVE_ID, MERCHANT_ID);
    }

    @Test
    void testCheckInitiativeValidPeriod_ValidNullInitiative() {
        // Given
        LocalDate today = LocalDate.now();

        // When & Then
        assertDoesNotThrow(() -> CommonCreationServiceImpl.checkInitiativeValidPeriod(today, null, "FLOW_NAME"));
    }

    private TransactionCreationRequest createDummyRequest(Long amountCents) {
        TransactionCreationRequest request = new TransactionCreationRequest();
        request.setInitiativeId(INITIATIVE_ID);
        request.setAmountCents(amountCents);
        return request;
    }

    private InitiativeConfig createDummyInitiativeConfig(InitiativeRewardType type, LocalDate startDate, LocalDate endDate) {
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeRewardType(type);
        initiativeConfig.setStartDate(startDate);
        initiativeConfig.setEndDate(endDate);
        return initiativeConfig;
    }
}