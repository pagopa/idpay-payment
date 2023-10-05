package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonPreAuthServiceImplTest {

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
    TestUtils.checkNotNullFields(result, "elaborationDateTime", "reward");

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
    TestUtils.checkNotNullFields(result, "elaborationDateTime", "reward");

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void previewPaymentRejected() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientException result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void previewPaymentRejectedNoBudget() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID1);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void previewPaymentNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("NO_ACTIVE_INITIATIVES"));

    when(rewardCalculatorConnectorMock.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientException result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
      commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any(), anyString());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        commonPreAuthService.relateUser(trx, "USERID2")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), "USERID2");
  }

  @Test
  void relateUserTrxExpired() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
            commonPreAuthService.relateUser(trx, USER_ID1)
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUser_statusSuspendedException() {
    // Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    WalletDTO walletDTO  = WalletDTOFaker.mockInstance(1, "SUSPENDED");
    String trxCode = trx.getTrxCode();

    when(walletConnectorMock.getWallet(trx.getInitiativeId(), USER_ID1))
            .thenReturn(walletDTO);

    // When
    ClientExceptionWithBody exception = Assertions.assertThrows(ClientExceptionWithBody.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, exception.getCode());
    assertEquals(String.format("User %s has been suspended for initiative %s",USER_ID1, trx.getInitiativeId()), exception.getMessage());

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
    ClientExceptionWithBody exception = Assertions.assertThrows(ClientExceptionWithBody.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
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
    ClientExceptionWithBody exception = Assertions.assertThrows(ClientExceptionWithBody.class,
            () -> commonPreAuthService.relateUser(trx, USER_ID1));

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
    assertEquals(String.format("Cannot relate transaction [%s] in status %s", trx.getTrxCode(), trx.getStatus()), exception.getMessage());

    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }


  void previewOtherException() {

    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));

    when(walletConnectorMock.getWallet(any(), any())).thenThrow(new RuntimeException());

    ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            commonPreAuthService.previewPayment(trx, "CHANNEL")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, result.getCode());

  }

}
