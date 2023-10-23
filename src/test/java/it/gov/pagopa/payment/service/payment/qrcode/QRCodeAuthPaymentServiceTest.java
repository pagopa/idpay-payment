package it.gov.pagopa.payment.service.payment.qrcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.TransactionRejectedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserSuspendedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
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
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock private TransactionInProgressRepository repositoryMock;
  @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
  @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock private TransactionNotifierService notifierServiceMock;
  @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
  @Mock private AuditUtilities auditUtilitiesMock;
  @Mock private WalletConnector walletConnectorMock;

  QRCodeAuthPaymentService service;

  private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";

  @BeforeEach
  void setUp() {
    service =
            new QRCodeAuthPaymentServiceImpl(
                    repositoryMock,
                    qrCodeAuthorizationExpiredServiceMock,
                    rewardCalculatorConnectorMock,
                    notifierServiceMock,
                    paymentErrorNotifierServiceMock,
                    auditUtilitiesMock,
                    walletConnectorMock);
  }

  @Test
  void authPayment() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

    Reward reward = RewardFaker.mockInstance(1);
    reward.setCounters(new RewardCounters());

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

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

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    assertEquals(authPaymentDTO, result);
    TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor");
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    verify(notifierServiceMock).notify(any(TransactionInProgress.class), anyString());
  }

  @Test
  void authPaymentWhenRejected() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("DUMMYREJECTIONREASON"));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);


    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(authPaymentDTO.getStatus());
              transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
              return transaction;
            })
        .when(repositoryMock)
        .updateTrxRejected(Mockito.eq(transaction.getId()), Mockito.eq(authPaymentDTO.getRejectionReasons()),
            Mockito.argThat(trxChargeDate -> trxChargeDate.isAfter(transaction.getTrxDate())));

    TransactionRejectedException result =
            assertThrows(TransactionRejectedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock, times(1)).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");

    Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());
  }

  @Test
  void authPaymentWhenRejectedNoBudget() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    Mockito.doAnswer(
                    invocationOnMock -> {
                      transaction.setStatus(authPaymentDTO.getStatus());
                      transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                      return transaction;
                    })
            .when(repositoryMock)
            .updateTrxRejected(Mockito.eq(transaction.getId()), Mockito.eq(authPaymentDTO.getRejectionReasons()),
                Mockito.argThat(trxChargeDate -> trxChargeDate.isAfter(transaction.getTrxDate())));

    BudgetExhaustedException result =
            assertThrows(BudgetExhaustedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");

    Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());
  }

  @Test
  void authPaymentNotFound() {
    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(null);

    TransactionNotFoundOrExpiredException result =
        assertThrows(TransactionNotFoundOrExpiredException.class, () -> service.authPayment("USERID1", "trxcode1"));

    assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
  }

  @Test
  void authPaymentUserNotAssociatedWithTrx() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

    UserNotAllowedException result =
        assertThrows(UserNotAllowedException.class, () -> service.authPayment("userId", "trxcode1"));

    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ANOTHER_USER, result.getCode());
  }

  @Test
  void authPaymentAuthorized() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    transaction.setUserId("USERID%d".formatted(1));
    transaction.setReward(10L);
    transaction.setRejectionReasons(Collections.emptyList());

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    TransactionAlreadyAuthorizedException result =
            assertThrows(TransactionAlreadyAuthorizedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");

    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, result.getCode());
  }

  @Test
  void authPaymentStatusKo() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    OperationNotAllowedException result =
        assertThrows(OperationNotAllowedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");

    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, result.getCode());
  }

  @Test
  void authPaymentOtherException() {
    TransactionInProgress transaction =
            TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenThrow(new RuntimeException());

    Assertions.assertThrows(RuntimeException.class, () -> service.authPayment("USERID1", "trxcode1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"SUSPENDED", "UNSUBSCRIBED"})
  void authPayment_walletStatusSuspended(String walletStatus) {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, walletStatus);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    try {
      service.authPayment("USERID1", "trxcode1");
      Assertions.fail("Expected exception");
    } catch (UserSuspendedException | UserNotOnboardedException e) {
      assertNotNull(e);
      if(PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, e.getCode());
      } else {
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, e.getCode());
      }
    }

    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
  }
}
