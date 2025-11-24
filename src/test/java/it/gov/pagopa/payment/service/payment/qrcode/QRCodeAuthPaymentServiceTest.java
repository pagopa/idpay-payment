package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
  @Mock private CommonAuthServiceImpl commonAuthServiceMock;
  QRCodeAuthPaymentService service;

  @BeforeEach
  void setUp() {
    service =
            new QRCodeAuthPaymentServiceImpl(
                    qrCodeAuthorizationExpiredServiceMock,
                    commonAuthServiceMock);
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

    when(qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode()))
        .thenReturn(transaction);

    when(commonAuthServiceMock.authPayment(transaction, "USERID1", "trxcode1"))
            .thenReturn(authPaymentDTO);

    AuthPaymentDTO result = service.authPayment("USERID1", "trxcode1");


    assertNotNull(result);
    assertEquals(authPaymentDTO, result);
    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired(any());
    verify(qrCodeAuthorizationExpiredServiceMock).findByTrxCodeAndAuthorizationNotExpired(any());

    TestUtils.checkNotNullFields(result, "rejectionReasons", "secondFactor","splitPayment",
            "residualAmountCents");
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    assertTrue(result.getRejectionReasons().isEmpty());
    assertEquals(Collections.emptyList(), result.getRejectionReasons());
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
}
