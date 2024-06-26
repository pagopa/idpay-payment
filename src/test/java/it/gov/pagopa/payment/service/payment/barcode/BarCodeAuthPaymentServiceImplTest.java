package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeAuthPaymentServiceImplTest {

    @Mock private BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private MerchantConnector merchantConnector;
    @Mock private CommonAuthServiceImpl commonAuthServiceMock;
    private static final String USER_ID = "USERID1";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String TRX_CODE1 = "trxcode1";
    private static final String ACQUIRER_ID = "ACQUIRER_ID";
    private static final long AMOUNT_CENTS = 1000L;
    private static final String ID_TRX_ACQUIRER = "ID_TRX_ACQUIRER";
    private static final AuthBarCodePaymentDTO AUTH_BAR_CODE_PAYMENT_DTO = AuthBarCodePaymentDTO.builder()
            .amountCents(AMOUNT_CENTS)
            .idTrxAcquirer(ID_TRX_ACQUIRER)
            .build();

    BarCodeAuthPaymentServiceImpl barCodeAuthPaymentService;


    @BeforeEach
    void setup(){
        barCodeAuthPaymentService = new BarCodeAuthPaymentServiceImpl(
                barCodeAuthorizationExpiredServiceMock,
                merchantConnector,
                commonAuthServiceMock,
                auditUtilitiesMock);
    }

    @Test
    void barCodeAuthPayment(){
        // Given
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transaction.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());


        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
                .thenReturn(transaction);

        when(merchantConnector.merchantDetail(MERCHANT_ID, transaction.getInitiativeId()))
                .thenReturn(MerchantDetailDTO.builder().build());
        when(commonAuthServiceMock.invokeRuleEngine(transaction))
                .thenReturn(authPaymentDTO);
        // When
        AuthPaymentDTO result = barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(authPaymentDTO, result);
        verify(barCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1);
        TestUtils.checkNotNullFields(result, "rejectionReasons","splitPayment",
                "residualAmountCents");
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
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
                assertThrows(TransactionInvalidException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, authBarCodePaymentDTO, MERCHANT_ID, ACQUIRER_ID));

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
                assertThrows(TransactionNotFoundOrExpiredException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, ACQUIRER_ID));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
    }

    @Test
    void barCodeAuthPayment_TooManyRequestThrownByRewardCalculator() {
        // Given
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transaction.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
                .thenReturn(transaction);

        when(merchantConnector.merchantDetail(MERCHANT_ID, transaction.getInitiativeId()))
                .thenReturn(MerchantDetailDTO.builder().build());

        when(commonAuthServiceMock.invokeRuleEngine(transaction))
                .thenThrow(new TooManyRequestsException("Too many request on the ms reward",true,null));

        TooManyRequestsException result = Assertions.assertThrows(TooManyRequestsException.class,
                () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, AUTH_BAR_CODE_PAYMENT_DTO, MERCHANT_ID, ACQUIRER_ID));

        // Then
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TOO_MANY_REQUESTS, result.getCode());

    }
}
