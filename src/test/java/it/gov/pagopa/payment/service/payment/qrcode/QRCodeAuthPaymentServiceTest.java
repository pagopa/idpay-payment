package it.gov.pagopa.payment.service.payment.qrcode;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.messagescheduler.AuthorizationTimeoutSchedulerServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock private TransactionInProgressRepository repositoryMock;
  @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
  @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock private AuditUtilities auditUtilitiesMock;
  @Mock private WalletConnector walletConnectorMock;
  @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
  @Mock private AuthorizationTimeoutSchedulerServiceImpl timeoutSchedulerServiceMock;
  QRCodeAuthPaymentService service;

  private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";


  @BeforeEach
  void setUp() {
    service =
            new QRCodeAuthPaymentServiceImpl(
                    repositoryMock,
                    qrCodeAuthorizationExpiredServiceMock,
                    rewardCalculatorConnectorMock,
                    auditUtilitiesMock,
                    walletConnectorMock,
                    commonPreAuthServiceMock,
                    timeoutSchedulerServiceMock);
  }

  @Test
  void authPayment() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);
    authPaymentDTO.setRejectionReasons(Collections.emptyList());

    Reward reward = RewardFaker.mockInstance(1);
    reward.setCounters(new RewardCounters());

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    when(repositoryMock.updateTrxAuthorized(transaction, authPaymentDTO,
            CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), List.of())))
            .thenReturn(UpdateResult.acknowledged(1, 1L, null));

    doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    assertEquals(authPaymentDTO, result);
    TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor","splitPayment",
            "residualAmountCents");
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    assertTrue(result.getRejectionReasons().isEmpty());
    assertEquals(Collections.emptyList(), result.getRejectionReasons());
  }

  @Test
  void authPaymentButUpdateTrxFailed() {
    TransactionInProgress transaction =
            TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);
    authPaymentDTO.setRejectionReasons(Collections.emptyList());

    Reward reward = RewardFaker.mockInstance(1);
    reward.setCounters(new RewardCounters());

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    when(repositoryMock.updateTrxAuthorized(transaction, authPaymentDTO,
            CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), List.of())))
            .thenReturn(UpdateResult.acknowledged(0, 0L, null));

    doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
    assertEquals(List.of(PaymentConstants.PAYMENT_AUTHORIZATION_TIMEOUT), result.getRejectionReasons());
    assertEquals(Collections.emptyMap(), result.getRewards());
    assertNull(result.getReward());
    assertNull(result.getCounters());
  }

  @Test
  void authPaymentWhenRejected() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("DUMMYREJECTIONREASON"));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), authPaymentDTO.getRejectionReasons());

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(authPaymentDTO.getStatus());
              transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
              transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
              return transaction;
            })
        .when(repositoryMock)
        .updateTrxRejected(transaction, authPaymentDTO.getRejectionReasons(), initiativeRejectionReasons);

    doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

    TransactionRejectedException result =
            assertThrows(TransactionRejectedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock, times(1)).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
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
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), authPaymentDTO.getRejectionReasons());

    Mockito.doAnswer(
                    invocationOnMock -> {
                      transaction.setStatus(authPaymentDTO.getStatus());
                      transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                      transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
                      return transaction;
                    })
            .when(repositoryMock)
            .updateTrxRejected(transaction, authPaymentDTO.getRejectionReasons(),initiativeRejectionReasons);

    doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

    BudgetExhaustedException result =
            assertThrows(BudgetExhaustedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());
  }

  @Test
  void authPaymentWhenMismatchVersionCounter() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Map<String, List<String>> initiativeRejectionReasons = CommonPaymentUtilities.getInitiativeRejectionReason(transaction.getInitiativeId(), authPaymentDTO.getRejectionReasons());

    Mockito.doAnswer(
                    invocationOnMock -> {
                      transaction.setStatus(authPaymentDTO.getStatus());
                      transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                      transaction.setInitiativeRejectionReasons(initiativeRejectionReasons);
                      return transaction;
                    })
            .when(repositoryMock)
            .updateTrxRejected(transaction, authPaymentDTO.getRejectionReasons(),initiativeRejectionReasons);

    doNothing().when(timeoutSchedulerServiceMock).cancelScheduledMessage(1L);

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    Assertions.assertNotNull(result);
    Assertions.assertEquals(SyncTrxStatus.REJECTED, result.getStatus());
    Assertions.assertTrue(result.getRejectionReasons().contains(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));
  }

  @Test
  void authPaymentWhenRewardCalculatorReturn404() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(timeoutSchedulerServiceMock.scheduleMessage(transaction.getId())).thenReturn(1L);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenThrow(new TransactionNotFoundOrExpiredException("Resource not found on reward-calculator"));

    assertThrows(TransactionNotFoundOrExpiredException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired("trxcode1");
    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
    verify(timeoutSchedulerServiceMock, times(1)).scheduleMessage(transaction.getId());
    verify(timeoutSchedulerServiceMock, times(0)).cancelScheduledMessage(1L);
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

    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_ASSIGNED, result.getCode());
  }

  @Test
  void authPaymentAuthorized() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    transaction.setUserId("USERID%d".formatted(1));
    transaction.setReward(10L);
    transaction.setRejectionReasons(Collections.emptyList());

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

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

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,transaction);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(commonPreAuthServiceMock.previewPayment(transaction,transaction.getChannel(),SyncTrxStatus.AUTHORIZATION_REQUESTED)).thenReturn(authPaymentDTO);

    OperationNotAllowedException result =
        assertThrows(OperationNotAllowedException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");

    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED, result.getCode());
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

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
            .thenReturn(transaction);

    try {
      service.authPayment("USERID1", "trxcode1");
      Assertions.fail("Expected exception");
    } catch (UserSuspendedException | UserNotOnboardedException e) {
      if(PaymentConstants.WALLET_STATUS_SUSPENDED.equals(walletStatus)){
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, e.getCode());
      } else {
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, e.getCode());
      }
    }

    verify(walletConnectorMock, times(1)).getWallet(transaction.getInitiativeId(), "USERID1");
  }
}
