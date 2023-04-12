package it.gov.pagopa.payment.service.qrcode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentRequestDTOFaker;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QRCodePreAuthServiceImplTest {

  @Mock TransactionInProgressRepository transactionInProgressRepository;

  @Mock
  TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

  @Mock AuthPaymentRequestMapper authPaymentRequestMapper;

  @Mock RewardCalculatorConnector rewardCalculatorConnector;

  QRCodePreAuthService qrCodePreAuthService;

  @BeforeEach
  void setUp() {
    qrCodePreAuthService =
        new QRCodePreAuthServiceImpl(
            transactionInProgressRepository,
            transactionInProgress2TransactionResponseMapper,
            authPaymentRequestMapper,
            rewardCalculatorConnector);
  }

  @Test
  void relateUser() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
    AuthPaymentRequestDTO request = AuthPaymentRequestDTOFaker.mockInstance(1);
    AuthPaymentResponseDTO preview = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    TransactionResponse response = TransactionResponseFaker.mockInstance(1);

    when(transactionInProgressRepository.findByTrxCodeAndRelateUser("TRXCODE1", "USERID1")).thenReturn(trx);
    when(authPaymentRequestMapper.rewardMap(any())).thenReturn(request);
    when(rewardCalculatorConnector.previewTransaction(anyString(), any())).thenReturn(preview);
    when(transactionInProgress2TransactionResponseMapper.apply(any())).thenReturn(response);

    TransactionResponse result = qrCodePreAuthService.relateUser("TRXCODE1", "USERID1");

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "id", "merchantId");

    verify(transactionInProgressRepository, times(1)).updateTrxIdentified(anyString());
    verify(transactionInProgressRepository, times(0)).updateTrxRejected(anyString(), anyList());
  }

  @Test
  void relateUserNotOnboarded() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
    AuthPaymentRequestDTO request = AuthPaymentRequestDTOFaker.mockInstance(1);
    AuthPaymentResponseDTO preview = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.REJECTED);

    when(transactionInProgressRepository.findByTrxCodeAndRelateUser("TRXCODE1", "USERID1")).thenReturn(trx);
    when(authPaymentRequestMapper.rewardMap(any())).thenReturn(request);
    when(rewardCalculatorConnector.previewTransaction(anyString(), any())).thenReturn(preview);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
      qrCodePreAuthService.relateUser("TRXCODE1", "USERID1")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());

    verify(transactionInProgressRepository, times(0)).updateTrxIdentified(anyString());
    verify(transactionInProgressRepository, times(1)).updateTrxRejected(anyString(), anyList());
  }
}
