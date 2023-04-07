package it.gov.pagopa.payment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionCreatedMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreated;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionCreatedFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QRCodePaymentServiceTest {

  @Mock
  TransactionInProgress2TransactionCreatedMapper transactionInProgress2TransactionCreatedMapper;

  @Mock
  TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;

  @Mock RewardRuleRepository rewardRuleRepository;

  @Mock TransactionInProgressRepository transactionInProgressRepository;

  @Mock
  TrxCodeGenUtil trxCodeGenUtil;

  QRCodePaymentService qrCodePaymentService;

  @BeforeEach
  void setUp() {
    qrCodePaymentService =
        new QRCodePaymentServiceImpl(
            transactionInProgress2TransactionCreatedMapper,
            transactionCreationRequest2TransactionInProgressMapper,
            rewardRuleRepository,
            transactionInProgressRepository, trxCodeGenUtil);
  }

  @Test
  void createTransaction() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    TransactionCreated trxCreated = TransactionCreatedFaker.mockInstance(1);
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);

    when(rewardRuleRepository.checkIfExists("INITIATIVEID1")).thenReturn(true);
    when(transactionCreationRequest2TransactionInProgressMapper.apply(any(TransactionCreationRequest.class))).thenReturn(trx);
    when(transactionInProgress2TransactionCreatedMapper.apply(any(TransactionInProgress.class))).thenReturn(trxCreated);
    when(trxCodeGenUtil.get()).thenReturn("TRXCODE1");
    when(transactionInProgressRepository.existsByTrxCode("TRXCODE1")).thenReturn(false);

    TransactionCreated result = qrCodePaymentService.createTransaction(trxCreationReq);

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trxCreated, result);

    verify(transactionInProgressRepository, times(1)).save(any());

  }

  @Test
  void createTransactionNotFound() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);

    when(rewardRuleRepository.checkIfExists("INITIATIVEID1")).thenReturn(false);

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
      qrCodePaymentService.createTransaction(trxCreationReq)
    );

    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
    Assertions.assertEquals("NOT FOUND", ((ClientExceptionWithBody) result).getTitle());
  }
}
