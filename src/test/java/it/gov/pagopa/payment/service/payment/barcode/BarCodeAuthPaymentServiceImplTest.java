package it.gov.pagopa.payment.service.payment.barcode;

import com.azure.resourcemanager.monitor.models.OnboardingStatus;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.*;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeAuthPaymentServiceImplTest {

    @Mock
    private PaymentCheckService paymentCheckService;
    @Mock
    private BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredServiceMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private MerchantConnector merchantConnector;
    @Mock
    private CommonAuthServiceImpl commonAuthServiceMock;
    @Mock
    private DecryptRestConnector decryptRestConnector;
    @Mock
    private TransactionInProgressRepository transaction;
    private static final String USER_ID = "USERID1";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String POINTOFSALE_ID = "POS_ID";
    private static final String TRX_CODE1 = "trxcode1";
    private static final String ACQUIRER_ID = "ACQUIRER_ID";
    private static final long AMOUNT_CENTS = 1000L;
    private static final String ID_TRX_ACQUIRER = "ID_TRX_ACQUIRER";
    private static final AuthBarCodePaymentDTO AUTH_BAR_CODE_PAYMENT_DTO = AuthBarCodePaymentDTO.builder()
            .amountCents(AMOUNT_CENTS)
            .idTrxAcquirer(ID_TRX_ACQUIRER)
            .additionalProperties(Map.of("productGtin","123123"))
            .build();

    BarCodeAuthPaymentServiceImpl barCodeAuthPaymentService;


    @BeforeEach
    void setup() {
        barCodeAuthPaymentService = new BarCodeAuthPaymentServiceImpl(
                paymentCheckService,
                barCodeAuthorizationExpiredServiceMock,
                merchantConnector,
                transaction,
                commonAuthServiceMock,
                decryptRestConnector,
                auditUtilitiesMock);
    }

    @Test
    void barCodeAuthPayment() {
        // Given
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transactionInProgress.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(0, OnboardingStatus.ONBOARDED.getValue());

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());

        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .type(PointOfSaleTypeEnum.PHYSICAL)
                .franchiseName("Test Franchise")
                .businessName("Test Business")
                .fiscalCode("FISCALCODE123")
                .vatNumber("12345678901")
                .build();

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transactionInProgress.getTrxCode()))
                .thenReturn(transactionInProgress);

        when(merchantConnector.getPointOfSale(MERCHANT_ID, POINTOFSALE_ID))
                .thenReturn(pointOfSaleDTO);
        when(commonAuthServiceMock.invokeRuleEngine(transactionInProgress))
                .thenReturn(authPaymentDTO);
        when(commonAuthServiceMock.checkWalletStatusAndReturn(transactionInProgress.getInitiativeId(),transactionInProgress.getUserId()))
                .thenReturn(walletDTO);
        ProductDTO productDTO = ProductDTOFaker.mockInstance();

        when(paymentCheckService.validateProduct(any())).thenReturn(productDTO);

        // When
        AuthPaymentDTO result = barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID,POINTOFSALE_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(authPaymentDTO, result);
        verify(barCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1);
        TestUtils.checkNotNullFields(result, "rejectionReasons", "splitPayment",
                "residualAmountCents");
        assertEquals(transactionInProgress.getTrxCode(), result.getTrxCode());
    }

    @Test
    void barCodeAuthPayment_invalidAdditionalProperties() {
        // Given
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(100L)
                .idTrxAcquirer("")
                .build();

        // When
        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, authBarCodePaymentDTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.TRX_ADDITIONAL_PROPERTIES_NOT_EXIST, result.getCode());
    }

    @ParameterizedTest
    @ValueSource(longs = {-100, 0})
    void barCodeAuthPayment_invalidAmount(long amountCents) {
        // Given
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(amountCents)
                .idTrxAcquirer("")
                .build();

        // When
        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, authBarCodePaymentDTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.AMOUNT_NOT_VALID, result.getCode());
    }

    @Test
    void barCodeAuthPayment_trxNotFound() {
        // Given
        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1)).thenReturn(null);


        doThrow(new TransactionNotFoundOrExpiredException("DUMMY_EXCEPTION"))
                .when(commonAuthServiceMock).checkAuth(eq(TRX_CODE1), any());
        // When
        TransactionNotFoundOrExpiredException result =
                assertThrows(TransactionNotFoundOrExpiredException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
    }

    @Test
    void barCodeAuthPayment_TooManyRequestThrownByRewardCalculator() {
        // Given
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transactionInProgress.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1,OnboardingStatus.ONBOARDED.getValue());
        walletDTO.setFamilyId("familyId");

        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .type(PointOfSaleTypeEnum.PHYSICAL)
                .franchiseName("Test Franchise")
                .businessName("Test Business")
                .fiscalCode("FISCALCODE123")
                .vatNumber("12345678901")
                .build();

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transactionInProgress.getTrxCode()))
                .thenReturn(transactionInProgress);

        when(merchantConnector.getPointOfSale(MERCHANT_ID, POINTOFSALE_ID))
                .thenReturn(pointOfSaleDTO);

        when(commonAuthServiceMock.invokeRuleEngine(transactionInProgress))
                .thenThrow(new TooManyRequestsException("Too many request on the ms reward", true, null));

        when(commonAuthServiceMock.checkWalletStatusAndReturn(transactionInProgress.getInitiativeId(),transactionInProgress.getUserId()))
                .thenReturn(walletDTO);

        ProductDTO productDTO = ProductDTOFaker.mockInstance();
        when(paymentCheckService.validateProduct(any())).thenReturn(productDTO);

        TooManyRequestsException result = Assertions.assertThrows(TooManyRequestsException.class,
                () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        // Then
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TOO_MANY_REQUESTS, result.getCode());
        verify(auditUtilitiesMock, times(1)).logBarCodeErrorAuthorizedPayment(TRX_CODE1, MERCHANT_ID);

    }


    @Test
    void previewPayment_ok() {
        TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transaction.findByTrxCode(any())).thenReturn(Optional.of(transactionInProgress));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        when(commonAuthServiceMock.previewPayment(any(), any())).thenReturn(authPaymentDTO);

        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("Pii");
        when(decryptRestConnector.getPiiByToken(any())).thenReturn(decryptCfDTO);

        ProductDTO productDTO = ProductDTOFaker.mockInstance();
        when(paymentCheckService.validateProduct(any())).thenReturn(productDTO);

        assertNotNull(barCodeAuthPaymentService.previewPayment("gtin","trxCode", 90000L));
    }

    @Test
    void previewPayment_negativeReward() {
        TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transaction.findByTrxCode(any())).thenReturn(Optional.of(transactionInProgress));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        authPaymentDTO.setRewardCents(-100L);
        when(commonAuthServiceMock.previewPayment(any(), any())).thenReturn(authPaymentDTO);

        assertThrows(TransactionInvalidException.class, () ->
                barCodeAuthPaymentService.previewPayment("gtin", "trxCode", 90000L));
    }

    @Test
    void previewPayment_negativeResidualAmount() {
        TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transaction.findByTrxCode(any())).thenReturn(Optional.of(transactionInProgress));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        authPaymentDTO.setRewardCents(100L);
        when(commonAuthServiceMock.previewPayment(any(), any())).thenReturn(authPaymentDTO);

        assertThrows(TransactionInvalidException.class, () ->
                barCodeAuthPaymentService.previewPayment("gtin", "trxCode", 90L));
    }

    @Test
    void previewPayment_TransactionIsNull() {
        when(transaction.findByTrxCode(any())).thenReturn(Optional.empty());

        TransactionNotFoundOrExpiredException exceptionResult =
                assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                        barCodeAuthPaymentService.previewPayment("gtin", "trxCode", 95000L));

        assertEquals("PAYMENT_NOT_FOUND_OR_EXPIRED", exceptionResult.getCode());
    }

    @Test
    void barCodeAuthPayment_invalidAdditionalProperties_emptyProductGtin() {
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(100L)
                .idTrxAcquirer("ID")
                .additionalProperties(Map.of("productGtin", ""))
                .build();

        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () ->
                        barCodeAuthPaymentService.authPayment(TRX_CODE1, authBarCodePaymentDTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        assertEquals(PaymentConstants.ExceptionCode.TRX_ADDITIONAL_PROPERTIES_NOT_EXIST, result.getCode());
        verify(auditUtilitiesMock, times(1)).logBarCodeErrorAuthorizedPayment(TRX_CODE1, MERCHANT_ID);
    }

    @Test
    void barCodeAuthPayment_checkAuthThrowsException() {
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1))
                .thenReturn(transactionInProgress);

        doThrow(new TransactionInvalidException(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, "Invalid status"))
                .when(commonAuthServiceMock).checkAuth(eq(TRX_CODE1), any());

        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () ->
                        barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, result.getCode());
        verify(auditUtilitiesMock, times(1)).logBarCodeErrorAuthorizedPayment(TRX_CODE1, MERCHANT_ID);
    }

    @Test
    void barCodeAuthPayment_checkTrxStatusToInvokePreAuthThrowsException() {
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);

        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .type(PointOfSaleTypeEnum.PHYSICAL)
                .franchiseName("Test Franchise")
                .businessName("Test Business")
                .fiscalCode("FISCALCODE123")
                .vatNumber("12345678901")
                .build();

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1))
                .thenReturn(transactionInProgress);

        when(merchantConnector.getPointOfSale(MERCHANT_ID, POINTOFSALE_ID))
                .thenReturn(pointOfSaleDTO);

        ProductDTO productDTO = ProductDTOFaker.mockInstance();
        when(paymentCheckService.validateProduct(any())).thenReturn(productDTO);

        doThrow(new TransactionInvalidException(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, "Status error"))
                .when(commonAuthServiceMock).checkTrxStatusToInvokePreAuth(any());

        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () ->
                        barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));

        assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, result.getCode());
        verify(auditUtilitiesMock, times(1)).logBarCodeErrorAuthorizedPayment(TRX_CODE1, MERCHANT_ID);
    }

    @Test
    void barCodeAuthPayment_verifyTransactionFieldsAreSet() {
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transactionInProgress.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transactionInProgress);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .type(PointOfSaleTypeEnum.ONLINE)
                .franchiseName("Online Franchise")
                .businessName("Online Business")
                .fiscalCode("ONLINE123")
                .vatNumber("98765432109")
                .build();

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transactionInProgress.getTrxCode()))
                .thenReturn(transactionInProgress);

        when(merchantConnector.getPointOfSale(MERCHANT_ID, POINTOFSALE_ID))
                .thenReturn(pointOfSaleDTO);
        when(commonAuthServiceMock.invokeRuleEngine(transactionInProgress))
                .thenReturn(authPaymentDTO);

        ProductDTO productDTO = ProductDTOFaker.mockInstance();
        when(paymentCheckService.validateProduct(any())).thenReturn(productDTO);

        barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID);

        assertEquals(AMOUNT_CENTS, transactionInProgress.getAmountCents());
        assertEquals(AMOUNT_CENTS, transactionInProgress.getEffectiveAmountCents());
        assertEquals(ID_TRX_ACQUIRER, transactionInProgress.getIdTrxAcquirer());
        assertEquals(MERCHANT_ID, transactionInProgress.getMerchantId());
        assertEquals("Online Business", transactionInProgress.getBusinessName());
        assertEquals("ONLINE123", transactionInProgress.getMerchantFiscalCode());
        assertEquals("98765432109", transactionInProgress.getVat());
        assertEquals("Online Franchise", transactionInProgress.getFranchiseName());
        assertEquals("ONLINE", transactionInProgress.getPointOfSaleType());
        assertEquals(ACQUIRER_ID, transactionInProgress.getAcquirerId());
        assertEquals(PaymentConstants.CURRENCY_EUR, transactionInProgress.getAmountCurrency());
        assertEquals(POINTOFSALE_ID, transactionInProgress.getPointOfSaleId());
    }
}
