package it.gov.pagopa.payment.service.payment.barCode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BarCodeAuthPaymentServiceImplTest {

    @Mock
    private TransactionInProgressRepository repositoryMock;
    @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private WalletConnector walletConnectorMock;

    @Test
    void barCodeAuthPayment(){
        // Given
        BarCodeAuthPaymentServiceImpl barCodeAuthPaymentService = new BarCodeAuthPaymentServiceImpl(
                repositoryMock,
                qrCodeAuthorizationExpiredServiceMock,
                rewardCalculatorConnectorMock,
                notifierServiceMock,
                paymentErrorNotifierServiceMock,
                auditUtilitiesMock,
                walletConnectorMock);
        String trxCode = "trxcode1";
        String merchantId = "MERCHANT_ID";
        long amountCents = 1000;

        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transaction.setUserId("USERID1");

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        Reward reward = RewardFaker.mockInstance(1);
        reward.setCounters(new RewardCounters());

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");

        when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
                .thenReturn(transaction);

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
        AuthPaymentDTO result = barCodeAuthPaymentService.authPayment(trxCode, merchantId, amountCents);

        // Then
        verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
        verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
        assertEquals(authPaymentDTO, result);
        TestUtils.checkNotNullFields(result, "rejectionReasons");
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        verify(notifierServiceMock).notify(any(TransactionInProgress.class), anyString());
    }
}
