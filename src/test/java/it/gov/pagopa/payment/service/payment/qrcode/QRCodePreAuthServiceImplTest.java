package it.gov.pagopa.payment.service.payment.qrcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.TransactionRejectedException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.exception.custom.UserSuspendedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
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
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QRCodePreAuthServiceImplTest {

  @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
  @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock private AuditUtilities auditUtilitiesMock;
  @Mock private WalletConnector walletConnectorMock;

  private QRCodePreAuthService qrCodePreAuthService;

  private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";
  private static final String USER_ID1 = "USERID1";

  @BeforeEach
  void setUp() {
    long authorizationExpirationMinutes = 4350;
    qrCodePreAuthService =
            new QRCodePreAuthServiceImpl(
                    authorizationExpirationMinutes,
                    transactionInProgressRepositoryMock,
                    rewardCalculatorConnectorMock,
                    auditUtilitiesMock,
                    walletConnectorMock);
  }

  @Test
  void relateUser() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.ofNullable(trx));
    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", USER_ID1);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor","splitPayment",
            "residualAmountCents");

    verify(transactionInProgressRepositoryMock, times(1)).updateTrxWithStatusForPreview(any(), any(), any(),anyString(),any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet("INITIATIVEID1", USER_ID1);
  }

  @Test
  void relateUserIdentified() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);
    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", USER_ID1);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor","splitPayment",
            "residualAmountCents");

    verify(transactionInProgressRepositoryMock, times(1)).updateTrxWithStatusForPreview(any(), any(), any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserIdentifiedRejected() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);
    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    Assertions.assertNotNull(result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(), any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserIdentifiedRejectedNoBudget() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);
    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    BudgetExhaustedException result = Assertions.assertThrows(BudgetExhaustedException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(),any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("NO_ACTIVE_INITIATIVES"));
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);
    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
      qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    Assertions.assertNotNull(result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(), any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    UserNotAllowedException result = Assertions.assertThrows(UserNotAllowedException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID2")
    );

    Assertions.assertNotNull(result);

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(),any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), "USERID2");
  }

  @Test
  void relateUserTrxNotFound() {

    TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(), any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
  }

  @Test
  void relateUserTrxExpired() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    Assertions.assertNotNull(result);

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxWithStatusForPreview(any(), any(), any(), anyString(),any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList(), anyMap(), anyString());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserOtherException() {
    String errorMessageTest = "DUMMY_MESSAGE";

    Mockito.when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1"))
            .thenThrow(new RuntimeException(errorMessageTest));

    RuntimeException result = Assertions.assertThrows(RuntimeException.class,
            () -> qrCodePreAuthService.relateUser("trxcode1", USER_ID1));

    Assertions.assertNotNull(result);
    Assertions.assertEquals(errorMessageTest, result.getMessage());

  }

  @Test
  void relateUser_statusSuspendedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, "SUSPENDED");
    String trxCode = trx.getTrxCode();

    when(transactionInProgressRepositoryMock.findByTrxCode(trx.getTrxCode()))
            .thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    UserSuspendedException exception = Assertions.assertThrows(UserSuspendedException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, exception.getCode());
    assertEquals(String.format("The user has been suspended for initiative [%s]", trx.getInitiativeId()), exception.getMessage());

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(trx.getTrxCode());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);

  }

  @Test
  void relateUser_trxAlreadyAuthorizedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    String trxCode = trx.getTrxCode();

    when(transactionInProgressRepositoryMock.findByTrxCode(trx.getTrxCode()))
            .thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    TransactionAlreadyAuthorizedException exception = Assertions.assertThrows(TransactionAlreadyAuthorizedException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, exception.getCode());
    assertEquals(String.format("Transaction with transactionId [%s] is already authorized", trx.getId()), exception.getMessage());

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(trx.getTrxCode());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);

  }

  @Test
  void relateUser_trxStatusNotValidException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CANCELLED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);
    String trxCode = trx.getTrxCode();

    when(transactionInProgressRepositoryMock.findByTrxCode(trx.getTrxCode()))
            .thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    OperationNotAllowedException exception = Assertions.assertThrows(OperationNotAllowedException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED, exception.getCode());
    assertEquals(String.format("Cannot operate on transaction with transactionId [%s] in status %s", trx.getId(), trx.getStatus()), exception.getMessage());

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(trx.getTrxCode());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

}
