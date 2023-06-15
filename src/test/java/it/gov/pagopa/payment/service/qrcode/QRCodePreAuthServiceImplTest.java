package it.gov.pagopa.payment.service.qrcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class QRCodePreAuthServiceImplTest {

  @Mock private TransactionInProgressRepository transactionInProgressRepository;
  @Mock private RewardCalculatorConnector rewardCalculatorConnector;
  @Mock private AuditUtilities auditUtilitiesMock;

  private QRCodePreAuthService qrCodePreAuthService;

  @BeforeEach
  void setUp() {
    qrCodePreAuthService =
        new QRCodePreAuthServiceImpl(
            transactionInProgressRepository,
            rewardCalculatorConnector,
                auditUtilitiesMock);
  }

  @Test
  void relateUser() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");

    verify(transactionInProgressRepository, times(1)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserIdentified() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");

    verify(transactionInProgressRepository, times(1)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserIdentifiedRejected() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientException result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserIdentifiedRejectedNoBudget() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, result.getCode());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("NO_ACTIVE_INITIATIVES"));

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    ClientException result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
      qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId("USERID1");

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(trx);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID2")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserTrxNotFound() {

    when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1")).thenReturn(null);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString(), any(), any(), any());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserOtherException() {

    Mockito.when(transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1"))
            .thenThrow(new RuntimeException());

    try {
      qrCodePreAuthService.relateUser("trxcode1", "USERID1");
      Assertions.fail("Expected exception");
    } catch (ClientExceptionWithBody e) {
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
      Assertions.assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, e.getCode());
    }
  }
}
