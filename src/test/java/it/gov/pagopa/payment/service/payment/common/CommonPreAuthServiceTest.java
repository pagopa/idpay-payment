package it.gov.pagopa.payment.service.payment.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.TransactionRejectedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserSuspendedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonPreAuthServiceTest {

  @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
  @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock private AuditUtilities auditUtilitiesMock;
  @Mock private WalletConnector walletConnectorMock;

  private CommonPreAuthServiceImpl commonPreAuthService;

  private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";
  private static final String USER_ID1 = "USERID1";

  @BeforeEach
  void setUp() {
    long authorizationExpirationMinutes = 4350;
    commonPreAuthService =
            new CommonPreAuthServiceImpl(
                    authorizationExpirationMinutes,
                    transactionInProgressRepositoryMock,
                    rewardCalculatorConnectorMock,
                    auditUtilitiesMock,
                    walletConnectorMock);
  }

  @Test
  void relateUser() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    TransactionInProgress result = commonPreAuthService.relateUser(trx, USER_ID1);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "elaborationDateTime", "reward", "trxChargeDate");

    verify(walletConnectorMock, times(1)).getWallet("INITIATIVEID1", USER_ID1);
  }

  @Test
  void relateUserIdentified() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    TransactionInProgress result = commonPreAuthService.relateUser(trx, USER_ID1);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "elaborationDateTime", "reward", "trxChargeDate");

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void previewPaymentRejected() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertEquals("PAYMENT_GENERIC_REJECTED", result.getCode());
    Assertions.assertEquals("Transaction with trxCode [trxcode1] is rejected", result.getMessage());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void previewPaymentRejectedNoBudget() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    BudgetExhaustedException result = Assertions.assertThrows(BudgetExhaustedException.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void previewPaymentNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("NO_ACTIVE_INITIATIVES"));

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
      commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertEquals("PAYMENT_GENERIC_REJECTED", result.getCode());
    Assertions.assertEquals("Transaction with trxCode [trxcode1] is rejected", result.getMessage());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    UserNotAllowedException result = Assertions.assertThrows(UserNotAllowedException.class, () ->
        commonPreAuthService.relateUser(trx, "USERID2")
    );

    Assertions.assertNotNull(result);

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), "USERID2");
  }

  @Test
  void relateUserTrxExpired() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
            commonPreAuthService.relateUser(trx, USER_ID1)
    );

    Assertions.assertNotNull(result);

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUser_statusSuspendedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, "SUSPENDED");

    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    UserSuspendedException exception = Assertions.assertThrows(UserSuspendedException.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, exception.getCode());
    assertEquals(String.format("The user has been suspended for initiative [%s]", trx.getInitiativeId()), exception.getMessage());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);

  }

  @Test
  void relateUser_statusUnsubscribedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);

    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    UserNotOnboardedException exception = Assertions.assertThrows(UserNotOnboardedException.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, exception.getCode());
    assertEquals(String.format("The user has unsubscribed from initiative [%s]", trx.getInitiativeId()), exception.getMessage());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);

  }

  @Test
  void relateUser_trxAlreadyAuthorizedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    TransactionAlreadyAuthorizedException exception = Assertions.assertThrows(TransactionAlreadyAuthorizedException.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, exception.getCode());
    assertEquals(String.format("Transaction with trxCode [%s] is already authorized", trx.getTrxCode()), exception.getMessage());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);

  }

  @Test
  void relateUser_trxStatusNotValidException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CANCELLED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    OperationNotAllowedException exception = Assertions.assertThrows(OperationNotAllowedException.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
    assertEquals(String.format("Cannot relate transaction [%s] in status %s", trx.getTrxCode(), trx.getStatus()), exception.getMessage());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }


  @Test
  void preview_GenericRejected() {

    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));

    AuthPaymentDTO responseRE = AuthPaymentDTOFaker.mockInstance(1, trx);
    responseRE.setStatus(SyncTrxStatus.REJECTED);
    responseRE.setRejectionReasons(List.of("ANY_REASON_REJECTED"));
    when(rewardCalculatorConnectorMock.previewTransaction(any())).thenReturn(responseRE);

    TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

  }

}
