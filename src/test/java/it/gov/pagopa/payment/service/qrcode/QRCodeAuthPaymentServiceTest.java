package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.qrcode.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock private TransactionInProgressRepository repositoryMock;
  @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
  @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
  @Mock private TransactionNotifierService notifierServiceMock;
  @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
  @Mock private AuditUtilities auditUtilitiesMock;

  QRCodeAuthPaymentService service;

  @BeforeEach
  void setUp() {
    service =
            new QRCodeAuthPaymentServiceImpl(
                    repositoryMock,
                    qrCodeAuthorizationExpiredServiceMock,
                    rewardCalculatorConnectorMock,
                    notifierServiceMock,
                    paymentErrorNotifierServiceMock,
                    auditUtilitiesMock);
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

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    when(notifierServiceMock.notify(transaction, transaction.getUserId())).thenReturn(true);

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(SyncTrxStatus.AUTHORIZED);
              transaction.setReward(CommonUtilities.euroToCents(reward.getAccruedReward()));
              transaction.setRejectionReasons(List.of());
              return transaction;
            })
        .when(repositoryMock)
        .updateTrxAuthorized(transaction, CommonUtilities.euroToCents(reward.getAccruedReward()), List.of());

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");
    assertEquals(authPaymentDTO, result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");
    assertEquals(transaction.getTrxCode(), transaction.getTrxCode());
    verify(notifierServiceMock).notify(any(TransactionInProgress.class), anyString());
  }

  @Test
  void authPaymentWhenRejected() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("DUMMYREJECTIONREASON"));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(authPaymentDTO.getStatus());
              transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
              return transaction;
            })
        .when(repositoryMock)
        .updateTrxRejected(transaction.getId(), authPaymentDTO.getRejectionReasons());

    ClientException result =
            assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");

    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentWhenRejectedNoBudget() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(rewardCalculatorConnectorMock.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Mockito.doAnswer(
                    invocationOnMock -> {
                      transaction.setStatus(authPaymentDTO.getStatus());
                      transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                      return transaction;
                    })
            .when(repositoryMock)
            .updateTrxRejected(transaction.getId(), authPaymentDTO.getRejectionReasons());

    ClientException result =
            assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");

    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentNotFound() {
    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1")).thenReturn(null);

    ClientException result =
        assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));
    assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
    assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentUserNotAssociatedWithTrx() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);
    ClientException result =
        assertThrows(ClientException.class, () -> service.authPayment("userId", "trxcode1"));
    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ANOTHER_USER, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentAuthorized() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
    transaction.setUserId("USERID%d".formatted(1));
    transaction.setReward(10L);
    transaction.setRejectionReasons(Collections.emptyList());

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    ClientException result =
            assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));
    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ALREADY_AUTHORIZED, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentStatusKo() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);
    ClientException result =
        assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));
    assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_STATUS_NOT_VALID, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentOtherException() {
    TransactionInProgress transaction =
            TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
            .thenThrow(new RuntimeException());

    try {
      service.authPayment("USERID1", "trxcode1");
      Assertions.fail("Expected exception");
    } catch (ClientExceptionWithBody e) {
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
      Assertions.assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, e.getCode());
    }
  }
}
