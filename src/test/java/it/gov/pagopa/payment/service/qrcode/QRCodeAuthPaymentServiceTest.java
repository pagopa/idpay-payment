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
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock private TransactionInProgressRepository repository;
  @Mock private RewardCalculatorConnector rewardCalculatorConnector;
  @Mock private TransactionNotifierService notifierService;
  @Mock private PaymentErrorNotifierService paymentErrorNotifierService;
  @Mock private AuditUtilities auditUtilitiesMock;
  
  private final AuthPaymentMapper authPaymentMapper = new AuthPaymentMapper();

  QRCodeAuthPaymentService service;

  @BeforeEach
  void setUp() {
    service =
        new QRCodeAuthPaymentServiceImpl(
            repository,
            rewardCalculatorConnector,
            authPaymentMapper,
            notifierService,
            paymentErrorNotifierService,
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

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(rewardCalculatorConnector.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    when(notifierService.notify(transaction, transaction.getUserId())).thenReturn(true);

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(SyncTrxStatus.AUTHORIZED);
              transaction.setReward(CommonUtilities.euroToCents(reward.getAccruedReward()));
              transaction.setRejectionReasons(List.of());
              return transaction;
            })
        .when(repository)
        .updateTrxAuthorized(transaction, CommonUtilities.euroToCents(reward.getAccruedReward()), List.of());

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");

    verify(repository).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");
    assertEquals(authPaymentDTO, result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");
    assertEquals(transaction.getTrxCode(), transaction.getTrxCode());
    verify(notifierService).notify(any(TransactionInProgress.class), anyString());
  }

  @Test
  void authPaymentWhenRejected() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID1");

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    authPaymentDTO.setStatus(SyncTrxStatus.REJECTED);
    authPaymentDTO.setRejectionReasons(List.of("DUMMYREJECTIONREASON"));

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(rewardCalculatorConnector.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Mockito.doAnswer(
            invocationOnMock -> {
              transaction.setStatus(authPaymentDTO.getStatus());
              transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
              return transaction;
            })
        .when(repository)
        .updateTrxRejected(transaction.getId(), authPaymentDTO.getRejectionReasons());

    ClientException result =
            assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(repository).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");

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

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
            .thenReturn(transaction);

    when(rewardCalculatorConnector.authorizePayment(transaction)).thenReturn(authPaymentDTO);

    Mockito.doAnswer(
                    invocationOnMock -> {
                      transaction.setStatus(authPaymentDTO.getStatus());
                      transaction.setRejectionReasons(authPaymentDTO.getRejectionReasons());
                      return transaction;
                    })
            .when(repository)
            .updateTrxRejected(transaction.getId(), authPaymentDTO.getRejectionReasons());

    ClientException result =
            assertThrows(ClientException.class, () -> service.authPayment("USERID1", "trxcode1"));

    verify(repository).findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1");

    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.BUDGET_EXHAUSTED, ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentNotFound() {
    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1")).thenReturn(null);

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

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
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

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
        .thenReturn(transaction);

    AuthPaymentDTO result = service.authPayment(transaction.getUserId(), transaction.getTrxCode());
    assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");
  }

  @Test
  void authPaymentStatusKo() {
    TransactionInProgress transaction =
        TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
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

    when(repository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode()))
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
