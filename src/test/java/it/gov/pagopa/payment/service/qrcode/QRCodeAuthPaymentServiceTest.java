package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.connector.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthPaymentServiceTest {

  @Mock
  TransactionInProgressRepository transactionInProgressRepository;
  @Mock
  RewardCalculatorConnector rewardCalculatorConnector;
  @Mock
  AuthPaymentRequestMapper requestMapper;

  QRCodeAuthPaymentService service;

  @BeforeEach
  void setUp() {
    service = new QRCodeAuthPaymentServiceImpl(transactionInProgressRepository,
        rewardCalculatorConnector, requestMapper);
  }

  @Test
  void authPayment(){

  }
}
