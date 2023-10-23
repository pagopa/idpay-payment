package it.gov.pagopa.payment.service.payment.barcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.exception.custom.badrequest.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarCodeAuthPaymentServiceImplTest {

    @Mock
    private TransactionInProgressRepository repositoryMock;
    @Mock private BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredServiceMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private MerchantConnector merchantConnector;

    private static final String USER_ID = "USERID1";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String TRX_CODE1 = "trxcode1";

    BarCodeAuthPaymentServiceImpl barCodeAuthPaymentService;

    @BeforeEach
    void setup(){
        barCodeAuthPaymentService = new BarCodeAuthPaymentServiceImpl(
                repositoryMock,
                barCodeAuthorizationExpiredServiceMock,
                rewardCalculatorConnectorMock,
                notifierServiceMock,
                paymentErrorNotifierServiceMock,
                auditUtilitiesMock,
                walletConnectorMock,
                merchantConnector);
    }

    @Test
    void barCodeAuthPayment(){
        // Given
        long amountCents = 1000;

        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setUserId(USER_ID);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
                .thenReturn(transaction);

        when(merchantConnector.merchantDetail(MERCHANT_ID, transaction.getInitiativeId()))
                .thenReturn(MerchantDetailDTO.builder().build());

        when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

        when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

        when(notifierServiceMock.notify(transaction, transaction.getUserId())).thenReturn(true);

        Mockito.doAnswer(
                        invocationOnMock -> {
                            transaction.setStatus(SyncTrxStatus.AUTHORIZED);
                            transaction.setReward(CommonUtilities.euroToCents(reward.getAccruedReward()));
                            transaction.setRejectionReasons(List.of());
                            transaction.setTrxChargeDate(OffsetDateTime.now());
                            return transaction;
                        })
                .when(repositoryMock)
                .updateTrxAuthorized(transaction, CommonUtilities.euroToCents(reward.getAccruedReward()), List.of());

        // When
        AuthPaymentDTO result = barCodeAuthPaymentService.authPayment(TRX_CODE1, MERCHANT_ID, amountCents);

        // Then
        verify(barCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1);
        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), USER_ID);
        assertEquals(authPaymentDTO, result);
        TestUtils.checkNotNullFields(result, "rejectionReasons");
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        verify(notifierServiceMock).notify(any(TransactionInProgress.class), anyString());
    }

    @ParameterizedTest
    @ValueSource(longs = {-100, 0})
    void barCodeAuthPayment_invalidAmount(long amountCents) {
        // When
        TransactionInvalidException result =
                assertThrows(TransactionInvalidException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, MERCHANT_ID, amountCents));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.AMOUNT_NOT_VALID, result.getCode());
    }

    @Test
    void barCodeAuthPayment_trxNotFound() {
        // Given
        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE1)).thenReturn(null);

        // When
        TransactionNotFoundOrExpiredException result =
                assertThrows(TransactionNotFoundOrExpiredException.class, () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, MERCHANT_ID, 1000));

        // Then
        assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
    }
}
