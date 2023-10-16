package it.gov.pagopa.payment.service.payment.qrcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.custom.BadRequestException;
import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
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
    TestUtils.checkNotNullFields(result, "rejectionReasons");

    verify(transactionInProgressRepositoryMock, times(1)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
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
    TestUtils.checkNotNullFields(result, "rejectionReasons");

    verify(transactionInProgressRepositoryMock, times(1)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
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

    ForbiddenException result = Assertions.assertThrows(ForbiddenException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );


    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
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

    ForbiddenException result = Assertions.assertThrows(ForbiddenException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
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

    ForbiddenException result = Assertions.assertThrows(ForbiddenException.class, () ->
      qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    ForbiddenException result = Assertions.assertThrows(ForbiddenException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID2")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals("PAYMENT_USER_NOT_VALID", result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), "USERID2");
  }

  @Test
  void relateUserTrxNotFound() {

    NotFoundException result = Assertions.assertThrows(NotFoundException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserTrxExpired() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setTrxDate(OffsetDateTime.now().minusDays(5L));
    trx.setUserId(USER_ID1);
    WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

    when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1")).thenReturn(Optional.of(trx));
    when(walletConnectorMock.getWallet(any(), any())).thenReturn(walletDTO);

    NotFoundException result = Assertions.assertThrows(NotFoundException.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", USER_ID1)
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals("PAYMENT_NOT_FOUND_EXPIRED", result.getCode());

    verify(transactionInProgressRepositoryMock, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepositoryMock, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

  @Test
  void relateUserOtherException() {

    Mockito.when(transactionInProgressRepositoryMock.findByTrxCode("trxcode1"))
            .thenThrow(new RuntimeException());

    Assertions.assertThrows(RuntimeException.class, () -> qrCodePreAuthService.relateUser("trxcode1", USER_ID1));
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
    ForbiddenException exception = Assertions.assertThrows(ForbiddenException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.USER_SUSPENDED_ERROR, exception.getCode());
    assertEquals(String.format("User %s has been suspended for initiative %s",USER_ID1, trx.getInitiativeId()), exception.getMessage());

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
    ForbiddenException exception = Assertions.assertThrows(ForbiddenException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, exception.getCode());
    assertEquals(String.format("Transaction with trxCode [%s] is already authorized", trx.getTrxCode()), exception.getMessage());

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
    BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    // Then
    assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
    assertEquals(String.format("Cannot relate transaction [%s] in status %s", trx.getTrxCode(), trx.getStatus()), exception.getMessage());

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(trx.getTrxCode());
    verify(walletConnectorMock, times(1)).getWallet(trx.getInitiativeId(), USER_ID1);
  }

}
