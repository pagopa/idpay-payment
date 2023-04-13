package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.AuthPaymentRequestDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock
  TransactionInProgressRepository repository;
  @Mock
  RewardCalculatorConnector rewardCalculatorConnector;
  @Mock
  AuthPaymentMapper requestMapper;

  QRCodeAuthPaymentService service;

  @BeforeEach
  void setUp() {
    service = new QRCodeAuthPaymentServiceImpl(repository,
        rewardCalculatorConnector, requestMapper);
  }

  @Test
  void authPayment() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
    transaction.setUserId("USERID%d".formatted(1));
    AuthPaymentRequestDTO requestDTO = AuthPaymentRequestDTOFaker.mockInstance(1);
    Reward reward = RewardFaker.mockInstance(1);
    when(repository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(transaction.getTrxCode())).thenReturn(transaction);

    when(requestMapper.rewardMap(transaction)).thenReturn(requestDTO);

    when(rewardCalculatorConnector.authorizePayment(transaction,
        requestDTO)).thenReturn(authPaymentDTO);

    Mockito.doAnswer(invocationOnMock -> {
      transaction.setStatus(SyncTrxStatus.AUTHORIZED);
      transaction.setReward(reward);
      transaction.setRejectionReasons(List.of());
      return null;
    }).when(repository).updateTrxAuthorized(transaction.getId(),reward, List.of());

    AuthPaymentDTO result = service.authPayment("USERID1", "TRXCODE1");

    verify(repository).findByTrxCodeAndTrxChargeDateNotExpiredThrottled("TRXCODE1");
    assertEquals(authPaymentDTO, result);
    TestUtils.checkNotNullFields(result);
    assertEquals(transaction.getTrxCode(), transaction.getTrxCode());
  }

  @Test
  void authPaymentNotFound() {
    when(repository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled("TRXCODE1")).thenReturn(null);

    ClientException result =
        assertThrows(ClientException.class, () ->
            service.authPayment("USERID1", "TRXCODE1"));
    assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
    assertEquals("TRANSACTION NOT FOUND", ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentUserNotAssociatedWithTrx() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));

    when(repository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(transaction.getTrxCode())).thenReturn(transaction);
    ClientException result =
        assertThrows(ClientException.class, () -> service.authPayment(
            "userId", "TRXCODE1"));
    assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals("TRX USER ASSOCIATION", ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void authPaymentAuthorized() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.AUTHORIZED);
    transaction.setUserId("USERID%d".formatted(1));
    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);

    when(repository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(transaction.getTrxCode())).thenReturn(transaction);
    when(requestMapper.transactionMapper(any(TransactionInProgress.class))).thenReturn(authPaymentDTO);
    AuthPaymentDTO result = service.authPayment(transaction.getUserId(), transaction.getTrxCode());
    assertNotNull(result);
    TestUtils.checkNotNullFields(result);
  }

  @Test
  void authPaymentStatusKo() {
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.CREATED);
    transaction.setUserId("USERID%d".formatted(1));

    when(repository.findByTrxCodeAndTrxChargeDateNotExpiredThrottled(transaction.getTrxCode())).thenReturn(transaction);
    ClientException result =
        assertThrows(ClientException.class, () -> service.authPayment(
            "USERID1", "TRXCODE1"));
    assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
    Assertions.assertEquals("ERROR STATUS", ((ClientExceptionWithBody) result).getCode());
  }
}
