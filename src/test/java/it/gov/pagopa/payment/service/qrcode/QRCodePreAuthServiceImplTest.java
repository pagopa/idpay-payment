package it.gov.pagopa.payment.service.qrcode;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.TransactionSynchronousException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result);

    verify(transactionInProgressRepository, times(1)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserIdentified() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result);

    verify(transactionInProgressRepository, times(1)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserIdentifiedRejected() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    trx.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result);

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("NO_ACTIVE_INITIATIVES"));

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(trx);
    when(rewardCalculatorConnector.previewTransaction(trx)).thenReturn(authPaymentDTO);

    TransactionSynchronousException result = Assertions.assertThrows(TransactionSynchronousException.class, () ->
      qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserNotAuthorized() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    trx.setUserId("USERID1");

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(trx);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID2")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }

  @Test
  void relateUserTrxNotFound() {

    when(transactionInProgressRepository.findByTrxCodeAndTrxChargeDateNotExpired("trxcode1")).thenReturn(null);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString(), anyString());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyString(), anyList());
  }
}
